package com.example.demo.mypage.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;

public record FavoriteStoresRequest(
        @DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,
        @DecimalMin("-180") @DecimalMax("180") BigDecimal longitude,
        @Min(0) @Schema(defaultValue = "0") Integer page,
        @Min(1) @Max(100) @Schema(defaultValue = "10") Integer size) {

    public FavoriteStoresRequest {
        if (page == null) {
            page = 0;
        }
        if (size == null) {
            size = 10;
        }
    }

    @AssertTrue(message = "latitude와 longitude는 함께 입력해야 합니다")
    public boolean hasCompleteCoordinates() {
        return (latitude == null) == (longitude == null);
    }
}
