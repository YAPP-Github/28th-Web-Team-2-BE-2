package com.example.demo.report.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 인식 신뢰도. 0 이상 1 이하다.
 *
 * <p>{@code 0.96} 처럼 소수 둘째 자리로 정리해서 들고 다닌다. 범위 검사는 파서가 한다.
 */
public record AnalysisConfidence(BigDecimal value) {

    private static final int SCALE = 2;

    /**
     * 자릿수만 정리한다.
     *
     * <p>범위 검사는 하지 않는다 — 0~1 밖의 값은 {@code PriceTagResponseParser}가 이미 버린다.
     * 여기서 clamp 하면 모델이 0~100 스케일로 답했을 때 90 이 1.00 으로 승격된다.
     */
    public AnalysisConfidence {
        value = value.setScale(SCALE, RoundingMode.HALF_UP);
    }

    /** 근거가 약할 때 쓰는 값. 정책이 신뢰도를 깎을 때 이 값으로 내린다. */
    public static AnalysisConfidence low() {
        return new AnalysisConfidence(new BigDecimal("0.30"));
    }

    /**
     * 해석하지 못한 숫자가 남았으면 신뢰도를 낮춘다.
     *
     * <p>가격표에 {@code 250원}과 {@code 1인 10개 제한}이 함께 있으면 어느 숫자가 판매 가격인지
     * 사진만으로는 확정할 수 없다. 판정 기준은 "우리가 해석하지 못한 숫자가 남았는가"다 — 전체 숫자
     * 개수로 판정하면 {@code 감자 1kg 3900원}처럼 정상적인 가격표도 전부 걸려 신뢰도가 상수가 된다.
     *
     * <p>값을 버리지 않고 신뢰도만 낮춘다. 이미 더 낮게 나온 값은 그대로 두고({@code null}이면
     * {@code null}로 남긴다 — 없는 값을 만들면 "모름"과 "0.30을 측정했다"가 구별되지 않는다).
     */
    public static AnalysisConfidence downgradeIfAmbiguous(
            final AnalysisConfidence confidence, final int otherNumberCount) {
        if (otherNumberCount == 0 || confidence == null) {
            return confidence;
        }
        if (confidence.value.compareTo(low().value) < 0) {
            return confidence;
        }
        return low();
    }

}
