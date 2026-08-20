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
     * 단위만 바꾼 사본. 요청 표기({@code kg})를 품목 기준 단위({@code 1kg})로 맞춘 뒤
     * 저장하려고 쓴다 — 저장 표기가 섞이면 기존 제보와 비교할 수 없다.
     */
    public CreateUserReportCommand withUnit(final String unit) {
        return new CreateUserReportCommand(
                itemId, userId, regionId, price, unit, amount, reportType, storeId, store, photoUrl);
    }
}
