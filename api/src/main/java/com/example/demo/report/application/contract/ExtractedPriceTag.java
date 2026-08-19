package com.example.demo.report.application.contract;

import com.example.demo.report.domain.AnalysisConfidence;
import lombok.Builder;
import java.math.BigDecimal;

/**
 * 모델이 사진에서 읽어 낸 값. 아직 우리 품목·단위와 맞춰지지 않은 상태다.
 *
 * <p>Qwen JSON을 그대로 쓰지 않고 이 타입으로 좁힌 뒤 유스케이스로 넘긴다. presentation까지
 * 외부 응답 모양이 새지 않게 하는 경계다.
 *
 * <p>{@code otherNumberCount}를 뺀 모든 필드가 {@code null}일 수 있다. 인식하지 못한 값을 억지로
 * 채우지 않는다는 요구사항이 그대로 반영된 형태다.
 *
 * <p>{@code priceBasis}는 가격이 붙은 수량 표기다("3kg", "1개"). 이 값과 품목의 기본 단위가 다르면
 * 가격을 그 단위 값으로 쓸 수 없다 — 사용자가 확정해야 한다.
 *
 * <p>{@code otherNumberCount}는 가격·수량·기준으로 쓰지 않은 나머지 숫자의 개수다. 0보다 크면
 * 가격표에 우리가 해석하지 못한 숫자가 있다는 뜻이라 신뢰도를 깎는 근거가 된다.
 */
@Builder
public record ExtractedPriceTag(
        String itemName,
        AnalysisConfidence itemConfidence,
        Integer price,
        AnalysisConfidence priceConfidence,
        String priceBasis,
        BigDecimal amount,
        AnalysisConfidence amountConfidence,
        int otherNumberCount) {

    public static ExtractedPriceTag empty() {
        return new ExtractedPriceTag(null, null, null, null, null, null, null, 0);
    }

    public boolean hasItemName() {
        return itemName != null && !itemName.isBlank();
    }
}
