package com.example.demo.report.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

/**
 * 인식 결과. 사용자가 확인·수정한 뒤 제보 저장 API로 보낼 값이다.
 *
 * <p>{@code status}·{@code message}를 두지 않는다 — {@code ResponseWrapper}가 이미 모든
 * {@code /api/v1} 응답을 {@code {code, message, data}}로 감싼다.
 */
public record ImageAnalysisResponse(
        @Schema(description = "후보가 하나로 좁혀졌을 때만 채운다. 여럿이면 null이고 candidates를 쓴다")
        AnalyzedItem item,
        @Schema(description = "인식된 이름에 대응하는 품목 후보. 사용자가 고르게 한다")
        List<AnalyzedItem> candidates,
        AnalyzedPrice price,
        @Schema(description = "선택값. 사진에 근거가 없으면 null이다")
        AnalyzedAmount amount) {

    public record AnalyzedItem(
            @Schema(example = "12") Long itemId,
            @Schema(example = "오이") String name,
            @Schema(
                    description = "제보 저장 시 unit으로 보낼 값. 서버가 이 문자열과의 일치를 요구한다",
                    example = "1개")
            String unit,
            @Schema(description = "0~1. 참고용이며 최종 확인은 사용자가 한다", example = "0.96")
            BigDecimal confidence) {}

    public record AnalyzedPrice(
            @Schema(example = "250") Integer value,
            @Schema(description = "항상 KRW", example = "KRW") String currency,
            @Schema(
                    description = "가격표에 숫자가 여럿이면 낮아진다. 값이 있어도 사용자 확인이 필요하다",
                    example = "0.30")
            BigDecimal confidence) {

        public static final String KRW = "KRW";
    }

    public record AnalyzedAmount(
            @Schema(example = "1") BigDecimal value,
            @Schema(example = "0.72") BigDecimal confidence) {}
}
