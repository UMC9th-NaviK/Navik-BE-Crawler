package navik.growth.pipeline;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import navik.growth.analysis.dto.AnalysisDraft;
import navik.growth.analysis.dto.AnalysisRequest.GrowthAnalysisRequest;
import navik.growth.analysis.dto.AnalysisResponse.GrowthAnalysisResponse;
import navik.growth.analysis.service.GrowthAnalysisService;
import navik.growth.analysis.service.GrowthEmbeddingService;

@Slf4j
public class GrowthStageProcessor {
    // Global budget for stage starts, shared by every worker instance. Tool calls remain a separate provider budget.
    private static final DefaultRedisScript<Long> BUDGET = new DefaultRedisScript<>(
        "local n=redis.call('INCR',KEYS[1]); if n==1 then redis.call('PEXPIRE',KEYS[1],60000) end; "
        + "if n>tonumber(ARGV[1]) then return 0 end; return 1", Long.class);
    private final GrowthJobStore store;
    private final GrowthAnalysisService analysis;
    private final GrowthEmbeddingService embeddings;
    private final ObjectMapper mapper;
    private final StringRedisTemplate redis;
    private final ScheduledExecutorService heartbeats;
    private final int maxAttempts;
    private final int startsPerMinute;
    private final Duration maxRuntime;
    private final String embeddingModel;

    public GrowthStageProcessor(GrowthJobStore store, GrowthAnalysisService analysis, GrowthEmbeddingService embeddings,
        ObjectMapper mapper, StringRedisTemplate redis, ScheduledExecutorService heartbeats,
        int maxAttempts, int startsPerMinute, Duration maxRuntime, String embeddingModel) {
        this.store = store; this.analysis = analysis; this.embeddings = embeddings; this.mapper = mapper;
        this.redis = redis; this.heartbeats = heartbeats; this.maxAttempts = maxAttempts;
        this.startsPerMinute = startsPerMinute; this.maxRuntime = maxRuntime; this.embeddingModel = embeddingModel;
    }

    public boolean handle(String id, String stage) throws Exception {
        var acquired = store.acquire(id, stage);
        if (acquired.isEmpty()) {
            return store.get(id).map(job -> !"RUNNING".equals(job.state()) || !stage.equals(job.stage())).orElse(true);
        }
        var job = acquired.get();
        long start = System.nanoTime();
        var heartbeat = heartbeats.scheduleAtFixedRate(() -> {
            if (System.nanoTime() - start < maxRuntime.toNanos()) {
                try { store.heartbeat(job); }
                catch (Exception e) { log.warn("Job heartbeat failed: jobId={}", id); }
            }
        }, 10, 10, TimeUnit.SECONDS);
        try {
            Long allowed = redis.execute(BUDGET, List.of("{growth-v2}:budget:" + stage), Integer.toString(startsPerMinute));
            if (!Long.valueOf(1).equals(allowed)) return store.defer(job, Duration.ofSeconds(60));
            if (job.attempt() > maxAttempts) {
                return store.fail(job, true, "ATTEMPTS_EXHAUSTED", maxAttempts, Duration.ZERO);
            }
            String result;
            if ("ANALYZE".equals(stage)) {
                var request = mapper.readValue(job.input(), GrowthAnalysisRequest.class);
                result = mapper.writeValueAsString(analysis.analyzeDraft(request));
            } else {
                store.pinEmbeddingModel(job, embeddingModel);
                var draft = mapper.readValue(job.analysis(), AnalysisDraft.class);
                var checkpoint = job.result() == null ? null : mapper.readValue(job.result(), GrowthAnalysisResponse.class);
                result = mapper.writeValueAsString(embeddings.embed(draft, checkpoint, partial -> {
                    try { store.checkpoint(job, mapper.writeValueAsString(partial)); }
                    catch (JsonProcessingException e) { throw new IllegalArgumentException("Invalid checkpoint", e); }
                }));
            }
            if (System.nanoTime() - start >= maxRuntime.toNanos()) {
                return store.fail(job, false, "STAGE_DEADLINE_EXCEEDED", maxAttempts, Duration.ofSeconds(30));
            }
            boolean completed = store.complete(job, result);
            log.info("Growth stage finished: jobId={}, stage={}, attempt={}, committed={}, elapsedMs={}",
                id, stage, job.attempt(), completed, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
            return completed;
        } catch (Exception error) {
            boolean permanent = isPermanent(error);
            Duration delay = Duration.ofSeconds(Math.min(300, (1L << Math.min(job.attempt(), 8)) * 5)
                + ThreadLocalRandom.current().nextInt(5));
            Duration providerDelay = retryAfter(error);
            if (providerDelay.compareTo(delay) > 0) delay = providerDelay;
            log.warn("Growth stage failed: jobId={}, stage={}, attempt={}, permanent={}, error={}",
                id, stage, job.attempt(), permanent, error.getClass().getSimpleName());
            return store.fail(job, permanent, permanent ? "INVALID_INPUT_OR_RESULT" : "EXTERNAL_OR_STORAGE_ERROR",
                maxAttempts, delay);
        } finally {
            heartbeat.cancel(false);
        }
    }

    private boolean isPermanent(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            Integer status = statusCode(cause);
            if (status != null) return status >= 400 && status < 500 && status != 408 && status != 429;
            if (cause instanceof IllegalArgumentException || cause instanceof JsonProcessingException) return true;
        }
        return false;
    }
    private Integer statusCode(Throwable error) {
        if (error instanceof RestClientResponseException rest) return rest.getStatusCode().value();
        if (error instanceof WebClientResponseException web) return web.getStatusCode().value();
        return null;
    }

    static Duration retryAfter(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            org.springframework.http.HttpHeaders headers = null;
            if (cause instanceof RestClientResponseException rest) headers = rest.getResponseHeaders();
            if (cause instanceof WebClientResponseException web) headers = web.getHeaders();
            String value = headers == null ? null : headers.getFirst("Retry-After");
            if (value == null) continue;
            try { return Duration.ofSeconds(Math.max(0, Long.parseLong(value))); }
            catch (RuntimeException ignored) {
                try {
                    Duration delay = Duration.between(Instant.now(), ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant());
                    return delay.isNegative() ? Duration.ZERO : delay;
                } catch (RuntimeException invalidHeader) { return Duration.ZERO; }
            }
        }
        return Duration.ZERO;
    }

}
