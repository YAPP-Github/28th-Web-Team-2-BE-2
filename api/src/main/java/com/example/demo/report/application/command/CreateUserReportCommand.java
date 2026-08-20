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

    /**
     * 수량·단위·가격을 기준 단위 1개분으로 옮긴 사본. "2kg에 8000원"을 "1kg에 4000원"으로
     * 바꿔 저장하려고 쓴다 — 저장값이 같은 기준이어야 제보끼리, 공공가격과 비교할 수 있다.
     */
    public CreateUserReportCommand withQuantity(
            final String unit, final BigDecimal amount, final Integer price) {
        return new CreateUserReportCommand(
                itemId, userId, regionId, price, unit, amount, reportType, storeId, store, photoUrl);
    }
}
