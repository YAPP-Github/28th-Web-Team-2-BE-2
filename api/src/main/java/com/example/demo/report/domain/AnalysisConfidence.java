package com.example.demo.report.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 인식 신뢰도. 0 이상 1 이하다.
 *
 * <p>모델이 범위를 벗어난 값이나 이상한 정밀도를 주는 경우가 있어 여기서 한 번 정리한다. 값을
 * 버리지 않고 범위로 끌어당기는 이유는, 신뢰도는 참고용이고 최종 확인은 사용자가 하기 때문이다 —
 * 신뢰도 하나가 이상하다고 인식 결과 전체를 버리는 편이 사용자에게 더 나쁘다.
 */
public record AnalysisConfidence(BigDecimal value) {

    private static final BigDecimal MIN = BigDecimal.ZERO;
    private static final BigDecimal MAX = BigDecimal.ONE;
    private static final int SCALE = 2;

    public AnalysisConfidence {
        value = normalize(value);
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

    private static BigDecimal normalize(final BigDecimal raw) {
        if (raw == null) {
            return MIN;
        }
        final BigDecimal scaled = raw.setScale(SCALE, RoundingMode.HALF_UP);
        if (scaled.compareTo(MIN) < 0) {
            return MIN;
        }
        if (scaled.compareTo(MAX) > 0) {
            return MAX;
        }
        return scaled;
    }
}
