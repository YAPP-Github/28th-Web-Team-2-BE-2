package com.example.demo.report.application.contract;

/**
 * 인식된 품목명에 대응하는 품목 후보.
 *
 * <p>{@code item} 도메인의 Entity를 그대로 가져오지 않는다({@code docs/ARCHITECTURE.md} §7).
 * report가 필요한 세 값만 가진 Contract를 report가 소유하고, Adapter가 변환해 채운다.
 *
 * <p>{@code defaultUnit}을 함께 싣는 이유가 이 기능의 핵심 제약이다. 제보 저장 API는
 * {@code unit}이 {@code items.default_unit}과 문자열까지 같아야 통과시킨다
 * ({@code CreateUserReportUseCase.validateUnit}). 즉 단위는 모델이 추측할 값이 아니라
 * 품목이 결정하는 값이다.
 *
 * <p>모델 문자열과의 비교는 이 타입이 소유한다. 정규화 규칙이 호출부마다 흩어지면 한쪽만 바뀌어도
 * 신호가 없다 — 품목 조회는 못 맞히고 단위 일치는 거짓을 답한다.
 */
public record ItemCandidate(Long itemId, String name, String defaultUnit) {

    /** DB 의 품목명·단위에는 공백이 없다. 모델이 준 문자열의 공백만 지우고 비교한다. */
    public static String normalize(final String modelText) {
        if (modelText == null) {
            return "";
        }
        return modelText.replaceAll("\\s+", "");
    }

    /** 사진의 가격 기준({@code "1kg"})이 이 품목의 기본 단위와 같은지. */
    public boolean matchesUnit(final String priceBasis) {
        return defaultUnit.equals(normalize(priceBasis));
    }
}
