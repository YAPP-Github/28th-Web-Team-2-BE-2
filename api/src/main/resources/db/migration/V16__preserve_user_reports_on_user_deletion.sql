ALTER TABLE user_reports
    ALTER COLUMN user_id DROP NOT NULL;

ALTER TABLE user_reports
    DROP CONSTRAINT fk_user_reports_user;

ALTER TABLE user_reports
    ADD CONSTRAINT fk_user_reports_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL;

CREATE UNIQUE INDEX uk_user_reports_submission
    ON user_reports (user_id, item_id, COALESCE(store_id, 0), report_date, report_type)
    WHERE report_type IS NOT NULL;
