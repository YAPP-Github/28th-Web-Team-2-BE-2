package com.example.demo.item.presentation.dto;

import com.example.demo.item.application.query.PublicPricePeriod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record ItemPublicPriceRequest(
        @NotBlank
        @Schema(description = "법정동 코드", example = "1121510100") String regionId,
        @Schema(description = "조회 기간", defaultValue = "MONTH") PublicPricePeriod period) {

    public ItemPublicPriceRequest {
        period = defaultPeriod(period);
    }

    private static PublicPricePeriod defaultPeriod(final PublicPricePeriod period) {
        if (period == null) {
            return PublicPricePeriod.MONTH;
        }
        return period;
    }
}
