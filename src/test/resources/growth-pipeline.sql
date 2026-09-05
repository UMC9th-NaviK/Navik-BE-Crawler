-- Apply before enabling GROWTH_LOG_EVALUATION_MODE=stream.
-- Both services use this task schema; crawler must not receive business-table grants.
CREATE TABLE IF NOT EXISTS growth_analysis_job (
    id varchar(36) PRIMARY KEY,
    user_id bigint NOT NULL,
    growth_log_id bigint NOT NULL,
    processing_token varchar(36) NOT NULL,
    input_json text NOT NULL,
    analysis_json text,
    embedding_model varchar(100),
    result_json text,
    stage varchar(16) NOT NULL DEFAULT 'ANALYZE',
    state varchar(16) NOT NULL DEFAULT 'READY',
    attempt integer NOT NULL DEFAULT 0,
    next_attempt_at timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    lease_token varchar(36),
    lease_until timestamp with time zone,
    error_code varchar(80),
    created_at timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at timestamp with time zone,
    UNIQUE (growth_log_id, processing_token)
);
CREATE INDEX IF NOT EXISTS growth_analysis_job_status ON growth_analysis_job(state, updated_at);
CREATE INDEX IF NOT EXISTS growth_analysis_job_owner ON growth_analysis_job(user_id, growth_log_id, created_at);
CREATE TABLE IF NOT EXISTS growth_analysis_outbox (
    id varchar(36) PRIMARY KEY,
    job_id varchar(36) NOT NULL REFERENCES growth_analysis_job(id),
    stage varchar(16) NOT NULL,
    available_at timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at timestamp with time zone,
    lease_token varchar(36),
    lease_until timestamp with time zone,
    created_at timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS growth_analysis_outbox_due ON growth_analysis_outbox(published_at, available_at);
