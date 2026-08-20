package com.example.demo.store.application.result;

import java.time.Instant;
import java.time.LocalDate;

public record StoreReportSummary(
        long cheapItemCount,
        long expensiveItemCount,
        long totalReportedItemCount,
        LocalDate latestReportedDate,
        Instant latestReportedAt) {}
