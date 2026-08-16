package com.example.demo.region.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record NearbyRegionRequest(
        @Schema(description = "위도", example = "36.8358")
        @NotNull
        @DecimalMin(value = "-90")
        @DecimalMax(value = "90")
        BigDecimal latitude,
        @Schema(description = "경도", example = "127.1324")
        @NotNull
        @DecimalMin(value = "-180")
        @DecimalMax(value = "180")
        BigDecimal longitude) {}
