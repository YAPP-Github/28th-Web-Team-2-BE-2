package com.example.demo.report.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateUserReportRequest(
        @NotBlank @Pattern(regexp = "\\d{10}")
        @Schema(description = "법정동 코드", example = "1121510100") String regionId,
        @NotBlank @Pattern(regexp = "PURCHASE|OBSERVED")
        @Schema(
                description = "제보 유형",
                allowableValues = {"PURCHASE", "OBSERVED"},
                example = "PURCHASE")
        String reportType,
        @NotNull @Positive Integer price,
        @NotBlank @Size(max = 20) String unit,
        @NotNull @Positive @Digits(integer = 7, fraction = 3) BigDecimal amount,
        @Positive @Schema(description = "기존 매장 ID", example = "7", nullable = true) Long storeId,
        @Valid StoreRequest store,
        @Size(max = 500) String photoUrl) {

    public record StoreRequest(
            @NotBlank @Size(max = 30) String id,
            @NotBlank @Size(max = 100) String placeName,
            @Size(max = 500) String placeUrl,
            @Size(max = 255) String categoryName,
            @NotBlank @Size(max = 255) String addressName,
            @Size(max = 255) String roadAddressName,
            @Size(max = 30) String phone,
            @Size(max = 20) String categoryGroupCode,
            @Size(max = 50) String categoryGroupName,
            @Digits(integer = 3, fraction = 14) BigDecimal x,
            @Digits(integer = 3, fraction = 14) BigDecimal y,
            @PositiveOrZero Integer distance) {}
}
