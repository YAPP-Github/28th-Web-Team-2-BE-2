package com.example.demo.report.application.result;

import java.time.LocalDate;

public record DailyReportResult(
        LocalDate reportedAt, boolean hasReported, Long itemId, String itemName) {

    public static DailyReportResult empty(final LocalDate date) {
        return new DailyReportResult(date, false, null, null);
    }
}
