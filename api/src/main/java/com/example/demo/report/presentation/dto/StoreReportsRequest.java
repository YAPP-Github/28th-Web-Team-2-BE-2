package com.example.demo.report.presentation.dto;

import com.example.demo.report.application.query.ReportFilter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record StoreReportsRequest(
        @Schema(description = "CHEAP, EXPENSIVE 또는 전체 목록 ALL", defaultValue = "CHEAP") ReportFilter filter,
        @Min(0) @Schema(defaultValue = "0") Integer page,
        @Min(1) @Max(100) @Schema(defaultValue = "20") Integer size) {

    public StoreReportsRequest {
        if (filter == null) {
            filter = ReportFilter.CHEAP;
        }
        if (page == null) {
            page = 0;
        }
        if (size == null) {
            size = 20;
        }
    }
}
