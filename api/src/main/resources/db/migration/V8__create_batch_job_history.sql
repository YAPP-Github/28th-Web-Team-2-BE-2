DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM online_prices
        GROUP BY item_id, channel_id, created_at, product_url
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'V8 requires unique online price scope and product URL';
    END IF;
END
$$;

CREATE TABLE batch_job_execution (
    job_execution_id BIGSERIAL PRIMARY KEY,
    job_name VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    ended_at TIMESTAMP WITH TIME ZONE,
    total_records INTEGER NOT NULL DEFAULT 0,
    success_records INTEGER NOT NULL DEFAULT 0,
    error_message VARCHAR(1000),
    CONSTRAINT chk_batch_job_execution_status
        CHECK (status IN ('STARTED', 'COMPLETED', 'PARTIAL', 'FAILED')),
    CONSTRAINT chk_batch_job_execution_counts
        CHECK (total_records >= 0 AND success_records >= 0 AND success_records <= total_records)
);

CREATE TABLE batch_item_errors (
    error_id BIGSERIAL PRIMARY KEY,
    job_execution_id BIGINT NOT NULL,
    item_id BIGINT NOT NULL,
    channel_id INTEGER NOT NULL,
    attempt_count INTEGER NOT NULL,
    error_type VARCHAR(100) NOT NULL,
    error_message VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_batch_item_errors_job
        FOREIGN KEY (job_execution_id)
        REFERENCES batch_job_execution (job_execution_id) ON DELETE CASCADE,
    CONSTRAINT chk_batch_item_errors_attempt_count CHECK (attempt_count > 0),
    CONSTRAINT uq_batch_item_errors_execution_item_channel
        UNIQUE (job_execution_id, item_id, channel_id)
);

ALTER TABLE online_prices
    ADD CONSTRAINT uq_online_prices_scope_product_url
    UNIQUE NULLS NOT DISTINCT (item_id, channel_id, created_at, product_url);
