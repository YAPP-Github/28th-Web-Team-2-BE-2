package com.example.demo.store.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Objects;

public record NearbyStoreRequest(
        @NotNull
        @DecimalMin("-90.0")
        @DecimalMax("90.0")
        @Schema(description = "지도 중심 위도", example = "37.5088")
        BigDecimal latitude,
        @NotNull
        @DecimalMin("-180.0")
        @DecimalMax("180.0")
        @Schema(description = "지도 중심 경도", example = "127.0632")
        BigDecimal longitude,
        @Min(0)
        @Max(5000)
        @Schema(description = "검색 반경(미터)", example = "2000", defaultValue = "2000")
        Integer radius,
        @Schema(description = "단골 가게만 조회할지 여부", defaultValue = "false")
        Boolean onlyLiked,
        @Schema(description = "가게명 검색어", example = "장보고 마트")
        String keyword) {

    private static final int DEFAULT_RADIUS = 2000;

    public NearbyStoreRequest {
        radius = radius == null ? DEFAULT_RADIUS : radius;
        onlyLiked = onlyLiked != null && onlyLiked;
        keyword = Objects.requireNonNullElse(keyword, "").strip();
    }
}
