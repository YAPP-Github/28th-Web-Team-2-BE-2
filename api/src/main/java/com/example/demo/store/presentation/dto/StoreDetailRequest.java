package com.example.demo.store.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;

public record StoreDetailRequest(
        @DecimalMin("-90.0")
        @DecimalMax("90.0")
        @Schema(description = "거리 계산에 사용할 위도", example = "37.5088", nullable = true)
        BigDecimal latitude,
        @DecimalMin("-180.0")
        @DecimalMax("180.0")
        @Schema(description = "거리 계산에 사용할 경도", example = "127.0632", nullable = true)
        BigDecimal longitude) {

    @AssertTrue(message = "latitude와 longitude는 함께 전달해야 합니다")
    @Schema(hidden = true)
    public boolean isCoordinatePairValid() {
        return (latitude == null) == (longitude == null);
    }
}
