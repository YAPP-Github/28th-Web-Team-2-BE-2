package com.example.demo.report.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record UpdateUserReportRequest(
        @NotNull @Positive
        @Schema(description = "제보 가격", example = "3600") Integer price,
        @NotBlank @Size(max = 20)
        @Schema(description = "품목 기준 단위", example = "1kg") String unit,
        @NotNull @Positive @Digits(integer = 7, fraction = 3)
        @Schema(description = "제보 수량", example = "2.000") BigDecimal amount) {}
