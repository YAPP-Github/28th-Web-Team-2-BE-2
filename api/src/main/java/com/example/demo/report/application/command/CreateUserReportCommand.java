package com.example.demo.report.application.command;

import java.math.BigDecimal;

public record CreateUserReportCommand(
        Long itemId,
        Long userId,
        Integer price,
        String unit,
        BigDecimal amount,
        StoreSnapshot store,
        String photoUrl) {}
