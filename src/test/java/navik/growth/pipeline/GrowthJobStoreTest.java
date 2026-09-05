package navik.growth.pipeline;

import static org.assertj.core.api.Assertions.*;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.support.TransactionTemplate;

class GrowthJobStoreTest {
    private JdbcTemplate jdbc;
    private GrowthJobStore store;
    private String id;

    @BeforeEach
    void setup() {
        String url = System.getenv("PIPELINE_TEST_JDBC_URL");
        DriverManagerDataSource dataSource;
        if (url != null && !url.isBlank()) {
            var admin = new DriverManagerDataSource(url, "pipeline_test", "");
            String schema = "test_" + UUID.randomUUID().toString().replace("-", "");
            new JdbcTemplate(admin).execute("CREATE SCHEMA " + schema);
            dataSource = new DriverManagerDataSource(url + (url.contains("?") ? "&" : "?") + "currentSchema=" + schema, "pipeline_test", "");
        } else {
            dataSource = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "");
        }
        new ResourceDatabasePopulator(new ClassPathResource("growth-pipeline.sql")).execute(dataSource);
        jdbc = new JdbcTemplate(dataSource);
        store = new GrowthJobStore(jdbc, new TransactionTemplate(new DataSourceTransactionManager(dataSource)), Duration.ofSeconds(90));
        id = UUID.randomUUID().toString();
        jdbc.update("INSERT INTO growth_analysis_job(id,user_id,growth_log_id,processing_token,input_json) VALUES (?,1,2,?, '{}')", id, UUID.randomUUID().toString());
    }

    @Test
    void onlyOneWorkerAcquiresAndExpiredOwnerCannotCommit() {
        var old = store.acquire(id, "ANALYZE").orElseThrow();
        assertThat(store.acquire(id, "ANALYZE")).isEmpty();
        jdbc.update("UPDATE growth_analysis_job SET lease_until = ? WHERE id = ?", Timestamp.from(Instant.now().minusSeconds(5)), id);
        var current = store.acquire(id, "ANALYZE").orElseThrow();
        assertThat(store.complete(old, "old")).isFalse();
        assertThat(store.complete(current, "new")).isTrue();
        assertThat(store.get(id).orElseThrow().analysis()).isEqualTo("new");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM growth_analysis_outbox", Long.class)).isEqualTo(1);
    }

    @Test
    void resultAndNextOutboxRollBackTogether() {
        var job = store.acquire(id, "ANALYZE").orElseThrow();
        jdbc.execute("ALTER TABLE growth_analysis_outbox ADD CONSTRAINT reject_embedding CHECK (stage <> 'EMBED')");
        assertThatThrownBy(() -> store.complete(job, "draft")).isInstanceOf(RuntimeException.class);
        assertThat(store.get(id).orElseThrow().analysis()).isNull();
        assertThat(store.get(id).orElseThrow().state()).isEqualTo("RUNNING");
    }

    @Test
    void embeddingRetryKeepsAnalysisAndPartialVectors() {
        var analysis = store.acquire(id, "ANALYZE").orElseThrow();
        store.complete(analysis, "draft");
        var embedding = store.acquire(id, "EMBED").orElseThrow();
        store.checkpoint(embedding, "partial");
        assertThat(store.fail(embedding, false, "TEMPORARY", 3, Duration.ZERO)).isTrue();
        var resumed = store.acquire(id, "EMBED").orElseThrow();
        assertThat(resumed.analysis()).isEqualTo("draft");
        assertThat(resumed.result()).isEqualTo("partial");
        assertThat(resumed.attempt()).isEqualTo(2);
        assertThat(store.acquire(id, "ANALYZE")).isEmpty();
    }

    @Test
    void retryDeadlinePreventsEarlyReexecutionAndTerminalFailureEmitsResultAndDlq() {
        var job = store.acquire(id, "ANALYZE").orElseThrow();
        store.fail(job, false, "TEMPORARY", 3, Duration.ofHours(1));
        assertThat(store.acquire(id, "ANALYZE")).isEmpty();
        jdbc.update("UPDATE growth_analysis_job SET next_attempt_at = CURRENT_TIMESTAMP WHERE id = ?", id);
        var retry = store.acquire(id, "ANALYZE").orElseThrow();
        store.fail(retry, true, "INVALID", 3, Duration.ofHours(1));
        assertThat(jdbc.queryForObject("SELECT next_attempt_at <= CURRENT_TIMESTAMP FROM growth_analysis_job WHERE id=?", Boolean.class, id)).isTrue();
        assertThat(store.get(id).orElseThrow().state()).isEqualTo("FAILED");
        assertThat(jdbc.queryForList("SELECT stage FROM growth_analysis_outbox", String.class)).contains("APPLY", "DLQ");
    }

    @Test
    void expiredLeaseCannotBeRenewedAndBudgetDeferralDoesNotSpendAttempts() {
        var job = store.acquire(id, "ANALYZE").orElseThrow();
        store.defer(job, Duration.ZERO);
        assertThat(store.get(id).orElseThrow().attempt()).isZero();
        var next = store.acquire(id, "ANALYZE").orElseThrow();
        jdbc.update("UPDATE growth_analysis_job SET lease_until = ? WHERE id = ?", Timestamp.from(Instant.now().minusSeconds(5)), id);
        assertThat(store.heartbeat(next)).isFalse();
        assertThatThrownBy(() -> store.checkpoint(next, "stale")).isInstanceOf(IllegalStateException.class);
    }
    @Test
    void modelVersionCannotChangeWhileReusingEmbeddingCheckpoint() {
        var analyze = store.acquire(id, "ANALYZE").orElseThrow();
        store.complete(analyze, "draft");
        var embed = store.acquire(id, "EMBED").orElseThrow();
        store.pinEmbeddingModel(embed, "model-a");
        store.checkpoint(embed, "partial");
        assertThatThrownBy(() -> store.pinEmbeddingModel(embed, "model-b")).isInstanceOf(IllegalArgumentException.class);
        assertThat(store.get(id).orElseThrow().result()).isEqualTo("partial");
    }

}
