package com.example.demo.report.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/**
 * 인식 결과. 사용자가 확인·수정한 뒤 제보 저장 API로 보낼 값이다.
 *
 * <p>{@code status}·{@code message}를 두지 않는다 — {@code ResponseWrapper}가 이미 모든
 * {@code /api/v1} 응답을 {@code {code, message, data}}로 감싼다.
 */
public record ImageAnalysisResponse(
        @Schema(description = "품목을 찾았을 때만 채운다. 못 찾으면 null 이고 사용자가 직접 고른다")
        AnalyzedItem item,
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

    /**
     * 사진에서 읽은 가격.
     *
     * <p>{@code basis}는 그 가격이 붙어 있던 수량 표기다. {@code item.unit}과 다르면 이 가격을
     * 그 단위 기준값으로 쓸 수 없다 — {@code unitMatched}가 false 로 온다.
     */
    public record AnalyzedPrice(
            @Schema(example = "9900") Integer value,
            @Schema(description = "항상 KRW", example = "KRW") String currency,
            @Schema(
                    description = "이 숫자가 판매 가격이라는 확신. 품목 신뢰도와 별개이며, "
                            + "가격표에 해석하지 못한 숫자가 남으면 낮아진다",
                    example = "0.30")
            BigDecimal confidence,
            @Schema(
                    description = "사진에 적힌 가격 기준 수량. 적혀 있지 않으면 null",
                    example = "3kg",
                    nullable = true)
            String basis,
            @Schema(
                    description = "basis 가 item.unit 과 같은지. false 면 가격을 그 단위 기준으로 "
                            + "환산하거나 사용자가 직접 입력해야 한다. basis 가 null 이면 null",
                    example = "false",
                    nullable = true)
            Boolean unitMatched) {

        public static final String KRW = "KRW";
    }

    public record AnalyzedAmount(
            @Schema(example = "1") BigDecimal value,
            @Schema(example = "0.72") BigDecimal confidence) {}
}
