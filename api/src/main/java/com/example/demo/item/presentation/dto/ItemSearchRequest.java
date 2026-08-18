package com.example.demo.item.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record ItemSearchRequest(
        @NotBlank
        @Schema(description = "품목명 검색어", example = "고추") String keyword,
        @NotBlank
        @Schema(description = "법정동 코드", example = "1121510100") String regionId,
        @Min(1)
        @Max(100)
        @Schema(description = "조회 개수", example = "30", defaultValue = "30") Integer limit,
        @Min(0)
        @Schema(description = "조회 시작 위치", example = "0", defaultValue = "0") Integer offset) {

    private static final int DEFAULT_LIMIT = 30;
    private static final int DEFAULT_OFFSET = 0;

    public ItemSearchRequest {
        keyword = strip(keyword);
        limit = defaultValue(limit, DEFAULT_LIMIT);
        offset = defaultValue(offset, DEFAULT_OFFSET);
    }

    private static String strip(final String value) {
        if (value == null) {
            return null;
        }
        return value.strip();
    }

    private static int defaultValue(final Integer value, final int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        return value;
    }
}
