package com.example.demo.report.application.result;

import java.util.List;

public record MyWeeklyReportResult(int totalReportedDays, List<DailyReportResult> dailyReports) {}
