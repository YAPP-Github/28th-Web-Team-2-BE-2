ALTER TABLE user_reports
    ALTER COLUMN store_id DROP NOT NULL,
    ADD COLUMN region_id VARCHAR(10),
    ADD COLUMN report_type VARCHAR(20);

ALTER TABLE user_reports
    ADD CONSTRAINT ck_user_reports_report_type
        CHECK (report_type IS NULL OR report_type IN ('PURCHASE', 'OBSERVED'));
