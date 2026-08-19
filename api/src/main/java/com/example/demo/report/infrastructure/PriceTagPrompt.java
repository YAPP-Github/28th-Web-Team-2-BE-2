package com.example.demo.report.infrastructure;

/**
 * 추출 지시문.
 *
 * <p>{@code response_format}의 지원 여부를 공식 문서로 확정하지 못했으므로 프롬프트에도 JSON만
 * 답하라고 명시한다. 둘 중 하나가 동작하지 않아도 나머지가 받쳐 준다.
 *
 * <p>단위(kg·개·망)를 묻지 않는 것이 의도다. 저장 API가 {@code items.default_unit}과 문자열 일치를
 * 요구해서 모델이 준 단위는 쓸 수 없다. 대신 {@code priceBasis}로 "가격이 어떤 수량에 붙었는가"를
 * 받아 사용자가 확정하게 한다.
 *
 * <p>키 이름은 {@link PriceTagSchema}가 소유한다 — 프롬프트와 파서가 갈라지지 않게.
 */
final class PriceTagPrompt {

    static final String SYSTEM = """
            너는 한국 농산물 가격표 사진에서 값을 읽는 도구다.
            반드시 아래 스키마를 만족하는 JSON 객체 하나만 출력한다. 설명, 코드펜스, 여는 말을 쓰지 않는다.

            %s

            규칙:
            - 사진에서 확인할 수 없는 값은 추측하지 말고 null로 둔다.
            - amount는 선택값이다. "1망", "2개"처럼 수량이 적혀 있을 때만 채운다.
            - priceBasis는 가격이 어떤 수량에 붙은 값인지다. "3kg 9900원"이면 "3kg",
              "1개 500원"이면 "1개". 기준이 안 적혀 있으면 null로 둔다. 추측하지 않는다.
            - otherNumberCount는 price·amount·priceBasis로 쓴 숫자를 '제외'한 나머지 개수다.
              예: "250원 / 1인 10개 제한" → price=250, priceBasis=null, 남는 숫자는 1과 10 → 2.
              예: "감자 1kg 3900원" → price=3900, priceBasis="1kg", 남는 숫자 없음 → 0.
            - 통화는 항상 원(KRW)으로 가정한다.
            """.formatted(PriceTagSchema.JSON_SHAPE);

    static final String USER = "이 사진의 가격표를 읽고 스키마대로 JSON만 출력해라.";

    private PriceTagPrompt() {
    }
}
