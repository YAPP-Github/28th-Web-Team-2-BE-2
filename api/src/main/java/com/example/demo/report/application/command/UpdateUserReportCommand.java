package com.example.demo.report.application.command;

import java.math.BigDecimal;

public record UpdateUserReportCommand(
        Long reportId,
        Long userId,
        Integer price,
        String unit,
        BigDecimal amount) {}
