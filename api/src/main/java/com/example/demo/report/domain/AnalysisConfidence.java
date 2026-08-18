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

    public boolean isLowerThan(final AnalysisConfidence other) {
        return value.compareTo(other.value) < 0;
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
