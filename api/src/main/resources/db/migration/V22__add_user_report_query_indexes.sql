-- 날짜 기준 제보 조회(내 제보 목록·주간 현황)가 타는 인덱스.
-- 기존 idx_user_reports_user_created_at 은 created_at 기준이라 report_date 정렬에 쓰이지 않는다.
CREATE INDEX idx_user_reports_user_report_date
    ON user_reports (user_id, report_date DESC, report_id DESC);

-- 동네 품목 제보 조회의 필터 조건.
CREATE INDEX idx_user_reports_item_region_unit
    ON user_reports (item_id, region_id, unit);
