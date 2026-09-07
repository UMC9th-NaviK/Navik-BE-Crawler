package navik.growth.pipeline;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

public class GrowthJobStore {
    public record Job(String id, String input, String analysis, String result, String stage, String state,
        String leaseToken, int attempt) { }
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;
    private final Duration lease;

    public GrowthJobStore(JdbcTemplate jdbc, TransactionTemplate transactions, Duration lease) {
        this.jdbc = jdbc;
        this.transactions = transactions;
        this.lease = lease;
        jdbc.queryForList("SELECT id, next_attempt_at, embedding_model FROM growth_analysis_job WHERE 1=0");
    }

    public Optional<Job> acquire(String id, String stage) {
        return transactions.execute(status -> {
            String token = UUID.randomUUID().toString();
            int count = jdbc.update("""
                UPDATE growth_analysis_job SET state = 'RUNNING', lease_token = ?, lease_until = ?,
                attempt = attempt + 1, updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND stage = ? AND finished_at IS NULL
                AND next_attempt_at <= CURRENT_TIMESTAMP
                AND (state = 'READY' OR (state = 'RUNNING' AND lease_until < CURRENT_TIMESTAMP))
                """, token, after(lease), id, stage);
            return count == 1 ? get(id) : Optional.empty();
        });
    }

    public Optional<Job> get(String id) {
        return jdbc.query("""
            SELECT id, input_json, analysis_json, result_json, stage, state, lease_token, attempt
            FROM growth_analysis_job WHERE id = ?
            """, (rs, index) -> new Job(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4),
                rs.getString(5), rs.getString(6), rs.getString(7), rs.getInt(8)), id).stream().findFirst();
    }

    public boolean heartbeat(Job job) {
        return jdbc.update("""
            UPDATE growth_analysis_job SET lease_until = ?, updated_at = CURRENT_TIMESTAMP
            WHERE id = ? AND lease_token = ? AND state = 'RUNNING' AND lease_until > CURRENT_TIMESTAMP
            """, after(lease), job.id(), job.leaseToken()) == 1;
    }

    public void pinEmbeddingModel(Job job, String model) {
        int updated = jdbc.update("""
            UPDATE growth_analysis_job SET embedding_model = ? WHERE id = ? AND lease_token = ?
            AND state = 'RUNNING' AND lease_until > CURRENT_TIMESTAMP
            AND (embedding_model IS NULL OR embedding_model = ?)
            """, model, job.id(), job.leaseToken(), model);
        if (updated != 1) throw new IllegalArgumentException("Embedding model changed or lease lost");
    }

    public void checkpoint(Job job, String result) {
        int count = jdbc.update("""
            UPDATE growth_analysis_job SET result_json = ?, updated_at = CURRENT_TIMESTAMP
            WHERE id = ? AND lease_token = ? AND state = 'RUNNING' AND lease_until > CURRENT_TIMESTAMP
            """, result, job.id(), job.leaseToken());
        if (count != 1) throw new IllegalStateException("Worker lease lost");
    }

    public boolean complete(Job job, String result) {
        return Boolean.TRUE.equals(transactions.execute(status -> {
            String column = "ANALYZE".equals(job.stage()) ? "analysis_json" : "result_json";
            String next = "ANALYZE".equals(job.stage()) ? "EMBED" : "APPLY";
            int count = jdbc.update("UPDATE growth_analysis_job SET " + column + " = ?, stage = ?, "
                + "state = 'READY', attempt = 0, lease_token = NULL, lease_until = NULL, error_code = NULL, "
                + "next_attempt_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP "
                + "WHERE id = ? AND lease_token = ? AND state = 'RUNNING' AND lease_until > CURRENT_TIMESTAMP",
                result, next, job.id(), job.leaseToken());
            if (count == 0) return false;
            publish(job.id(), next, Duration.ZERO);
            return true;
        }));
    }

    public boolean fail(Job job, boolean permanent, String code, int maxAttempts, Duration delay) {
        return Boolean.TRUE.equals(transactions.execute(status -> {
            boolean terminal = permanent || job.attempt() >= maxAttempts;
            int count = jdbc.update("""
                UPDATE growth_analysis_job SET state = ?, error_code = ?, lease_token = NULL, lease_until = NULL,
                next_attempt_at = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND lease_token = ? AND state = 'RUNNING' AND lease_until > CURRENT_TIMESTAMP
                """, terminal ? "FAILED" : "READY", code, after(terminal ? Duration.ZERO : delay), job.id(), job.leaseToken());
            if (count == 0) return false;
            publish(job.id(), terminal ? "APPLY" : job.stage(), terminal ? Duration.ZERO : delay);
            if (terminal) publish(job.id(), "DLQ", Duration.ZERO);
            return true;
        }));
    }

    public boolean defer(Job job, Duration delay) {
        return Boolean.TRUE.equals(transactions.execute(status -> {
            int count = jdbc.update("""
                UPDATE growth_analysis_job SET state = 'READY', attempt = attempt - 1,
                lease_token = NULL, lease_until = NULL, next_attempt_at = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND lease_token = ? AND state = 'RUNNING' AND lease_until > CURRENT_TIMESTAMP
                """, after(delay), job.id(), job.leaseToken());
            if (count == 0) return false;
            publish(job.id(), job.stage(), delay);
            return true;
        }));
    }

    private void publish(String id, String stage, Duration delay) {
        jdbc.update("INSERT INTO growth_analysis_outbox(id, job_id, stage, available_at) VALUES (?, ?, ?, ?)",
            UUID.randomUUID().toString(), id, stage, after(delay));
    }

    private Timestamp after(Duration duration) {
        return Timestamp.from(jdbc.queryForObject("SELECT CURRENT_TIMESTAMP", OffsetDateTime.class).toInstant().plus(duration));
    }
}
