package com.example.demo.report.application.result;

import java.time.Instant;

public record CreateUserReportResult(
        Long reportId,
        Long itemId,
        Long storeId,
        Instant reportedAt) {}
