package navik.pipeline;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import lombok.extern.slf4j.Slf4j;

/** Dedicated stream/group per stage. No payloads or vectors are carried in Redis. */
@Slf4j
public final class PipelineStreamConsumer implements AutoCloseable {
    @FunctionalInterface
    public interface Handler { boolean handle(String jobId) throws Exception; }
    private static final DefaultRedisScript<Long> ACK_DELETE = new DefaultRedisScript<>(
        "local n=redis.call('XACK',KEYS[1],ARGV[1],ARGV[2]); redis.call('XDEL',KEYS[1],ARGV[2]); return n", Long.class);
    private final StringRedisTemplate redis;
    private final String stream;
    private final String group;
    private final Duration claimIdle;
    private final Handler handler;
    private final ExecutorService executor;
    private final int concurrency;
    private volatile boolean stopped;

    public PipelineStreamConsumer(StringRedisTemplate redis, String stream, String group,
        int concurrency, Duration claimIdle, Handler handler) {
        if (concurrency < 1 || concurrency > 64 || claimIdle.compareTo(Duration.ofSeconds(10)) < 0) {
            throw new IllegalArgumentException("Invalid worker concurrency or claim interval");
        }
        this.redis = redis;
        this.stream = stream;
        this.group = group;
        this.concurrency = concurrency;
        this.claimIdle = claimIdle;
        this.handler = handler;
        this.executor = Executors.newFixedThreadPool(concurrency, Thread.ofPlatform().name(group + "-", 0).factory());
    }

    public void start() {
        for (int i = 0; i < concurrency; i++) executor.submit(this::run);
    }

    private void run() {
        var consumer = Consumer.from(group, UUID.randomUUID().toString());
        boolean initialized = false;
        while (!stopped && !Thread.currentThread().isInterrupted()) {
            try {
                if (!initialized) { ensureGroup(); initialized = true; }
                // Alternate recovery with a new read so failing jobs cannot starve new work.
                var pending = redis.opsForStream().pending(stream, group, Range.unbounded(), 100);
                if (pending != null) {
                    for (var entry : pending) {
                        if (entry.getElapsedTimeSinceLastDelivery().compareTo(claimIdle) >= 0) {
                            handle(redis.opsForStream().claim(stream, group, consumer.getName(), claimIdle, entry.getId()));
                            break;
                        }
                    }
                }
                handle(redis.opsForStream().read(consumer,
                    StreamReadOptions.empty().count(1).block(Duration.ofSeconds(1)),
                    StreamOffset.create(stream, ReadOffset.lastConsumed())));
            } catch (Exception e) {
                if (stopped) break;
                // Do not log payloads, vectors, or arbitrary provider response bodies.
                log.warn("Pipeline polling failed: stream={}, error={}", stream, e.getClass().getSimpleName());
                initialized = false;
                try { Thread.sleep(1000); }
                catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
            }
        }
    }

    private void handle(List<MapRecord<String, Object, Object>> records) throws Exception {
        if (records == null) return;
        for (var record : records) {
            var fields = record.getValue();
            String id = String.valueOf(fields.getOrDefault("jobId", ""));
            if (!"1".equals(String.valueOf(fields.get("schemaVersion"))) || !isUuid(id)) {
                // Preserve only envelope metadata in the dead-letter stream before removing a malformed entry.
                log.warn("Invalid pipeline envelope: stream={}, recordId={}", stream, record.getId());
                var dead = redis.opsForStream().add(MapRecord.create("{growth-v2}:dead",
                    java.util.Map.of("sourceStream", stream, "sourceId", record.getId().getValue(), "reason", "INVALID_ENVELOPE")));
                if (dead == null) throw new IllegalStateException("Cannot preserve invalid envelope");
                redis.execute(ACK_DELETE, List.of(stream), group, record.getId().getValue());
                continue;
            }
            if (handler.handle(id)) {
                redis.execute(ACK_DELETE, List.of(stream), group, record.getId().getValue());
            }
        }
    }

    public void ensureGroup() {
        try {
            redis.execute(new DefaultRedisScript<>(
                "return redis.call('XGROUP','CREATE',KEYS[1],ARGV[1],'0-0','MKSTREAM')", String.class),
                List.of(stream), group);
        } catch (RuntimeException e) {
            Throwable cause = e;
            while (cause != null) {
                if (cause.getMessage() != null && cause.getMessage().contains("BUSYGROUP")) return;
                cause = cause.getCause();
            }
            throw e;
        }
    }

    private static boolean isUuid(String value) {
        try { return UUID.fromString(value).toString().equals(value); }
        catch (IllegalArgumentException e) { return false; }
    }

    @Override
    public void close() { stopped = true; executor.shutdownNow(); }
}
