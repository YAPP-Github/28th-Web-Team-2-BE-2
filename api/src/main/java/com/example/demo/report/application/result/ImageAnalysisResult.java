package com.example.demo.report.application.result;

import com.example.demo.report.application.contract.ItemCandidate;
import com.example.demo.report.domain.AnalysisConfidence;
import java.math.BigDecimal;
import java.util.List;

/**
 * 인식 결과. 제보 화면이 폼에 채워 넣을 값이다.
 *
 * <p>여기에 {@code status}·{@code message}를 두지 않는다. {@code ResponseWrapper}가 모든
 * {@code /api/v1} 응답을 {@code {code, message, data}}로 감싸므로 payload에 또 두면 같은 정보가
 * 두 군데 생기고 어느 쪽이 정본인지 알 수 없게 된다.
 *
 * <p>인식하지 못한 값은 {@code null}이다. 특히 {@code amount}는 사진에 근거가 없으면 채우지 않는다.
 */
public record ImageAnalysisResult(
        ItemCandidate item,
        AnalysisConfidence itemConfidence,
        List<ItemCandidate> candidates,
        Integer price,
        AnalysisConfidence priceConfidence,
        String unit,
        BigDecimal amount,
        AnalysisConfidence amountConfidence) {

    /** 사진에서 아무것도 읽지 못한 경우. 사용자가 직접 입력하도록 빈 결과를 돌려준다. */
    public static ImageAnalysisResult empty() {
        return new ImageAnalysisResult(null, null, List.of(), null, null, null, null, null);
    }
}
