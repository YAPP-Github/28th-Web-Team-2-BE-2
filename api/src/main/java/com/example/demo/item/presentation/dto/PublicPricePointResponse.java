package com.example.demo.item.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

public record PublicPricePointResponse(
        @Schema(description = "가격 기준일", example = "2026-08-19") LocalDate date,
        @Schema(description = "해당 일자 공공가격") Integer price) {}
