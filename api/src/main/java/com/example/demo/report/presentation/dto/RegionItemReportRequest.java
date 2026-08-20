package com.example.demo.report.presentation.dto;

import com.example.demo.report.application.query.UserReportSort;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record RegionItemReportRequest(
        @Schema(description = "정렬 방식", defaultValue = "LATEST") UserReportSort sort,
        @Min(0)
        @Schema(description = "페이지 번호", example = "0", defaultValue = "0") Integer page,
        @Min(1)
        @Max(100)
        @Schema(description = "페이지 크기", example = "10", defaultValue = "10") Integer size) {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;

    public RegionItemReportRequest {
        sort = defaultSort(sort);
        page = defaultValue(page, DEFAULT_PAGE);
        size = defaultValue(size, DEFAULT_SIZE);
    }

    private static UserReportSort defaultSort(final UserReportSort sort) {
        if (sort == null) {
            return UserReportSort.LATEST;
        }
        return sort;
    }

    private static int defaultValue(final Integer value, final int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        return value;
    }
}
