package com.example.demo.report.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record MyReportRequest(
        @Min(0)
        @Schema(description = "페이지 번호", example = "0", defaultValue = "0") Integer page,
        @Min(1)
        @Max(100)
        @Schema(description = "페이지 크기", example = "10", defaultValue = "10") Integer size) {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;

    public MyReportRequest {
        page = Objects.requireNonNullElse(page, DEFAULT_PAGE);
        size = Objects.requireNonNullElse(size, DEFAULT_SIZE);
    }
}
