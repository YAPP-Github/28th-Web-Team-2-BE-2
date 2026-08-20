package com.example.demo.report.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record RegionLowestPriceReportsRequest(
        @Min(1) @Max(10) @Schema(defaultValue = "5") Integer limit) {

    public RegionLowestPriceReportsRequest {
        if (limit == null) {
            limit = 5;
        }
    }
}
