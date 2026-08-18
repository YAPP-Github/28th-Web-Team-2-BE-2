package com.example.demo.report.application.contract;

import com.example.demo.report.domain.AnalysisConfidence;
import java.math.BigDecimal;

/**
 * 모델이 사진에서 읽어 낸 값. 아직 우리 품목·단위와 맞춰지지 않은 상태다.
 *
 * <p>Qwen JSON을 그대로 쓰지 않고 이 타입으로 좁힌 뒤 유스케이스로 넘긴다. presentation까지
 * 외부 응답 모양이 새지 않게 하는 경계다.
 *
 * <p>모든 필드가 {@code null}일 수 있다. 인식하지 못한 값을 억지로 채우지 않는다는 요구사항이
 * 그대로 반영된 형태다. {@code numberCount}는 가격표에서 발견한 숫자의 개수로, 여러 숫자가 있을 때
 * 신뢰도를 깎는 판단 근거가 된다.
 */
public record ExtractedPriceTag(
        String itemName,
        AnalysisConfidence itemConfidence,
        Integer price,
        BigDecimal amount,
        AnalysisConfidence amountConfidence,
        int numberCount) {

    public static ExtractedPriceTag empty() {
        return new ExtractedPriceTag(null, null, null, null, null, 0);
    }

    public boolean hasItemName() {
        return itemName != null && !itemName.isBlank();
    }
}
