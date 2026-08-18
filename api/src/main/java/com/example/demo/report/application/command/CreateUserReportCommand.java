package com.example.demo.report.application.command;

import com.example.demo.report.domain.ReportType;
import java.math.BigDecimal;

public record CreateUserReportCommand(
        Long itemId,
        Long userId,
        String regionId,
        Integer price,
        String unit,
        BigDecimal amount,
        ReportType reportType,
        Long storeId,
        StoreSnapshot store,
        String photoUrl) {

    public CreateUserReportCommand(
            final Long itemId,
            final Long userId,
            final String regionId,
            final Integer price,
            final String unit,
            final BigDecimal amount,
            final ReportType reportType,
            final StoreSnapshot store,
            final String photoUrl) {
        this(itemId, userId, regionId, price, unit, amount, reportType, null, store, photoUrl);
    }
}
