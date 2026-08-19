CREATE INDEX idx_user_reports_store_report_date
    ON user_reports (store_id, report_date);

CREATE INDEX idx_store_favorites_store_id
    ON store_favorites (store_id);
