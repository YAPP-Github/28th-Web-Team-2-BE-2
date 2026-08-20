package com.example.demo.report.application.result;

import com.example.demo.report.application.contract.ItemCandidate;
import com.example.demo.report.domain.AnalysisConfidence;
import lombok.Builder;
import java.math.BigDecimal;

/**
 * 인식 결과. 제보 화면이 폼에 채워 넣을 값이다.
 *
 * <p>여기에 {@code status}·{@code message}를 두지 않는다. {@code ResponseWrapper}가 모든
 * {@code /api/v1} 응답을 {@code {code, message, data}}로 감싸므로 payload에 또 두면 같은 정보가
 * 두 군데 생기고 어느 쪽이 정본인지 알 수 없게 된다.
 *
 * <p>인식하지 못한 값은 {@code null}이다. 특히 {@code amount}는 사진에 근거가 없으면 채우지 않는다.
 *
 * <p>{@code priceBasis}는 사진에 적힌 가격 기준 수량("3kg")이고 {@code unit}은 품목의 기본 단위
 * ("1kg")다. 둘이 다르면 가격을 {@code unit} 기준값으로 쓸 수 없으므로 사용자가 확정해야 한다.
 */
@Builder
public record ImageAnalysisResult(
        ItemCandidate item,
        AnalysisConfidence itemConfidence,
        Integer price,
        AnalysisConfidence priceConfidence,
        String priceBasis,
        String unit,
        BigDecimal amount,
        AnalysisConfidence amountConfidence) {
}
