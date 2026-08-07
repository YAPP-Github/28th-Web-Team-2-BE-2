package com.example.demo.kamis.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

public record KamisDailyPriceRequest(
        @Schema(description = "가격 구분", example = "02", defaultValue = "02")
        @Pattern(regexp = "01|02", message = "productClsCode는 01 또는 02여야 합니다")
        String productClsCode,
        @Schema(description = "부류 코드", example = "200", defaultValue = "100")
        @Pattern(regexp = "100|200|300|400|500|600", message = "itemCategoryCode가 올바르지 않습니다")
        String itemCategoryCode,
        @Schema(description = "지역 코드", example = "1101")
        @Pattern(regexp = "\\d{4}", message = "countryCode는 네 자리 숫자여야 합니다")
        String countryCode,
        @Schema(description = "조회 날짜", example = "2015-10-01")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate regDay,
        @Schema(description = "kg 단위 환산 여부", example = "N", defaultValue = "N")
        @Pattern(regexp = "Y|N", message = "convertKgYn은 Y 또는 N이어야 합니다")
        String convertKgYn) {

    public KamisDailyPriceRequest {
        productClsCode = defaultValue(productClsCode, "02");
        itemCategoryCode = defaultValue(itemCategoryCode, "100");
        convertKgYn = defaultValue(convertKgYn, "N");
    }

    private static String defaultValue(final String value, final String defaultValue) {
        if (value != null) {
            return value;
        }
        return defaultValue;
    }
}
