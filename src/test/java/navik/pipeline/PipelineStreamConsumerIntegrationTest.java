package navik.pipeline;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.spring.data.connection.RedissonConnectionFactory;

@EnabledIfEnvironmentVariable(named = "PIPELINE_TEST_REDIS_PORT", matches = "[0-9]+")
class PipelineStreamConsumerIntegrationTest {
    private RedissonClient client;
    private StringRedisTemplate redis;
    private PipelineStreamConsumer worker;
    private String stream;
    private String group;

    @BeforeEach
    void setup() {
        int port = Integer.parseInt(System.getenv("PIPELINE_TEST_REDIS_PORT"));
        var config = new Config();
        config.useSingleServer().setAddress("redis://127.0.0.1:" + port);
        client = Redisson.create(config);
        redis = new StringRedisTemplate(new RedissonConnectionFactory(client));
        stream = "{growth-v2}:test:" + UUID.randomUUID(); group = "test-group";
    }

    @AfterEach
    void close() {
        if (worker != null) worker.close();
        redis.delete(stream);
        client.shutdown();
    }

    @Test
    void groupCreatedAfterPublishConsumesBacklogAndDeletesOnlyAfterSuccess() {
        var calls = new AtomicInteger();
        redis.opsForStream().add(MapRecord.create(stream, Map.of("schemaVersion", "1", "jobId", UUID.randomUUID().toString())));
        worker = new PipelineStreamConsumer(redis, stream, group, 1, Duration.ofSeconds(10), id -> { calls.incrementAndGet(); return true; });
        worker.start();
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            assertThat(calls.get()).isEqualTo(1);
            assertThat(redis.opsForStream().size(stream)).isZero();
            assertThat(redis.opsForStream().pending(stream, group).getTotalPendingMessages()).isZero();
        });
    }

    @Test
    void workerFailureLeavesPendingForRecovery() {
        worker = new PipelineStreamConsumer(redis, stream, group, 1, Duration.ofSeconds(10), id -> { throw new RuntimeException("crash"); });
        worker.ensureGroup();
        redis.opsForStream().add(MapRecord.create(stream, Map.of("schemaVersion", "1", "jobId", UUID.randomUUID().toString())));
        worker.start();
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
            assertThat(redis.opsForStream().pending(stream, group).getTotalPendingMessages()).isEqualTo(1));
        assertThat(redis.opsForStream().size(stream)).isEqualTo(1);
    }

    @Test
    void abandonedMessageIsClaimedAndProcessed() {
        var calls = new AtomicInteger();
        worker = new PipelineStreamConsumer(redis, stream, group, 1, Duration.ofSeconds(10), id -> { calls.incrementAndGet(); return true; });
        worker.ensureGroup();
        var record = redis.opsForStream().add(MapRecord.create(stream, Map.of("schemaVersion", "1", "jobId", UUID.randomUUID().toString())));
        redis.opsForStream().read(Consumer.from(group, "dead-worker"), StreamReadOptions.empty().count(1),
            StreamOffset.create(stream, ReadOffset.lastConsumed()));
        redis.execute(new org.springframework.data.redis.core.script.DefaultRedisScript<>(
            "return redis.call('XCLAIM',KEYS[1],ARGV[1],'dead-worker',0,ARGV[2],'IDLE',11000,'JUSTID')", java.util.List.class),
            java.util.List.of(stream), group, record.getValue());
        worker.start();
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            assertThat(calls.get()).isEqualTo(1);
            assertThat(redis.opsForStream().size(stream)).isZero();
        });
    }
    @Test
    void consumersReadOnlyUpToTheirExecutionCapacity() throws Exception {
        var active = new AtomicInteger();
        var peak = new AtomicInteger();
        var completed = new AtomicInteger();
        var gate = new java.util.concurrent.CountDownLatch(1);
        for (int i = 0; i < 6; i++) {
            redis.opsForStream().add(MapRecord.create(stream, Map.of("schemaVersion", "1", "jobId", UUID.randomUUID().toString())));
        }
        worker = new PipelineStreamConsumer(redis, stream, group, 2, Duration.ofSeconds(10), id -> {
            int now = active.incrementAndGet(); peak.accumulateAndGet(now, Math::max);
            try { gate.await(10, java.util.concurrent.TimeUnit.SECONDS); completed.incrementAndGet(); return true; }
            finally { active.decrementAndGet(); }
        });
        worker.start();
        try {
            await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> assertThat(active.get()).isEqualTo(2));
            assertThat(redis.opsForStream().pending(stream, group).getTotalPendingMessages()).isEqualTo(2);
        } finally { gate.countDown(); }
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertThat(completed.get()).isEqualTo(6));
        assertThat(peak.get()).isEqualTo(2);
    }

}
