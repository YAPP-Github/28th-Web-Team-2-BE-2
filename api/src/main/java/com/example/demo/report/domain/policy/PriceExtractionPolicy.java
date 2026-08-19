package com.example.demo.report.domain.policy;

import com.example.demo.report.domain.AnalysisConfidence;

/**
 * 인식 결과를 얼마나 믿을지 정하는 규칙.
 *
 * <p>요구사항이 지목한 상황이 근거다 — 가격표에 {@code 250원}과 {@code 1인 10개 제한}이 함께
 * 있으면 어느 숫자가 판매 가격인지 사진만으로는 확정할 수 없다.
 *
 * <p>판정 기준은 "우리가 해석하지 못한 숫자가 남았는가"({@code otherNumberCount > 0})다. 전체 숫자
 * 개수로 판정하면 {@code 감자 1kg 3900원}처럼 정상적인 가격표도 숫자가 둘이라 전부 걸려, 신뢰도가
 * 사실상 상수가 된다. 수량이 적힌 사진은 구조적으로 숫자가 둘 이상이므로 특히 그렇다.
 *
 * <p>값을 버리지 않고 신뢰도만 낮춘다. 후보를 보여 주고 사용자가 고치게 하는 편이 빈 화면을 주는
 * 것보다 낫다. 최종 확정은 항상 사용자가 한다.
 */
public final class PriceExtractionPolicy {

    private PriceExtractionPolicy() {
    }

    /**
     * 해석하지 못한 숫자가 남았으면 신뢰도를 낮춘다.
     *
     * <p>이미 더 낮게 나온 값은 그대로 둔다 — 정책이 모델보다 낙관적으로 판단하는 일이 없어야 한다.
     * 모델이 신뢰도를 주지 않았으면 {@code null}로 남긴다. 없는 값을 만들어 내면 "모름"과 "0.30을
     * 측정했다"가 구별되지 않는다.
     */
    public static AnalysisConfidence downgradeIfAmbiguous(
            final AnalysisConfidence confidence, final int otherNumberCount) {
        if (otherNumberCount == 0 || confidence == null) {
            return confidence;
        }
        if (confidence.isLowerThan(AnalysisConfidence.low())) {
            return confidence;
        }
        return AnalysisConfidence.low();
    }
}
