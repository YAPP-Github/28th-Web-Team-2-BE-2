package com.example.demo.report.domain.policy;

import com.example.demo.report.domain.AnalysisConfidence;

/**
 * 인식 결과를 얼마나 믿을지 정하는 규칙.
 *
 * <p>요구사항이 지목한 상황이 근거다 — 가격표에 {@code 250원}과 {@code 1인 10개 제한}이 함께
 * 있으면 어느 숫자가 판매 가격인지 사진만으로는 확정할 수 없다. 그럴 때 값을 그냥 내보내면
 * 사용자가 검토 없이 확정할 위험이 있어 신뢰도를 낮춘다.
 *
 * <p>값을 버리지 않고 신뢰도만 낮추는 쪽을 택했다. 후보를 보여 주고 사용자가 고치게 하는 편이
 * 빈 화면을 주는 것보다 낫다. 최종 확정은 항상 사용자가 한다.
 */
public final class PriceExtractionPolicy {

    /** 이 개수를 넘는 숫자가 있으면 판매 가격을 특정한 근거가 약하다고 본다. */
    private static final int AMBIGUOUS_NUMBER_THRESHOLD = 1;

    private PriceExtractionPolicy() {
    }

    public static boolean isPriceAmbiguous(final int numberCount) {
        return numberCount > AMBIGUOUS_NUMBER_THRESHOLD;
    }

    /**
     * 근거가 약하면 신뢰도를 낮춘다. 이미 더 낮게 나온 값은 그대로 둔다 — 정책이 모델보다
     * 낙관적으로 판단하는 일이 없어야 한다.
     */
    public static AnalysisConfidence downgradeIfAmbiguous(
            final AnalysisConfidence confidence, final int numberCount) {
        if (!isPriceAmbiguous(numberCount)) {
            return confidence;
        }
        if (confidence != null && confidence.isLowerThan(AnalysisConfidence.low())) {
            return confidence;
        }
        return AnalysisConfidence.low();
    }
}
