package com.example.demo.item.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record ItemQueryRequest(
        @NotBlank
        @Schema(description = "법정동 코드", example = "1121510100") String regionId,
        @Min(0)
        @Schema(description = "페이지 번호", example = "0", defaultValue = "0") Integer page,
        @Min(1)
        @Max(100)
        @Schema(description = "페이지 크기", example = "10", defaultValue = "10") Integer size) {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;

    public ItemQueryRequest {
        page = defaultValue(page, DEFAULT_PAGE);
        size = defaultValue(size, DEFAULT_SIZE);
    }

    private static int defaultValue(final Integer value, final int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        return value;
    }

}
