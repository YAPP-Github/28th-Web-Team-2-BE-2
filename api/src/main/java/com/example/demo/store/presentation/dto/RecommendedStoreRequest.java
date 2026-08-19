package com.example.demo.store.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

public record RecommendedStoreRequest(
        @NotBlank @Pattern(regexp = "\\d{10}")
        @Schema(description = "법정동 코드", example = "1121510100")
        String regionId,
        @DecimalMin("-90.0") @DecimalMax("90.0")
        @Schema(description = "추천 기준 위도", example = "37.5088")
        BigDecimal latitude,
        @DecimalMin("-180.0") @DecimalMax("180.0")
        @Schema(description = "추천 기준 경도", example = "127.0632")
        BigDecimal longitude,
        @Min(0) @Max(5000)
        @Schema(description = "검색 반경(미터)", example = "2000", defaultValue = "2000")
        Integer radius) {

    private static final int DEFAULT_RADIUS = 2000;

    public RecommendedStoreRequest {
        radius = radius == null ? DEFAULT_RADIUS : radius;
    }

    @AssertTrue(message = "latitude와 longitude는 함께 입력해야 합니다")
    public boolean hasCompleteCoordinates() {
        return (latitude == null) == (longitude == null);
    }
}
