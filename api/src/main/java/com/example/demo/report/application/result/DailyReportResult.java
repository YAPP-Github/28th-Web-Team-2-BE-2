package com.example.demo.report.application.result;

import java.time.LocalDate;

/** 주간 내 하루다. 제보가 없는 날도 포함되며 그때 {@code itemId}·{@code itemName}은 null이다. */
public record DailyReportResult(
        LocalDate date, boolean hasReported, Long itemId, String itemName) {}
