package com.example.demo.report.infrastructure;

/**
 * 추출 지시문.
 *
 * <p>{@code response_format}의 지원 여부를 공식 문서로 확정하지 못했으므로 프롬프트에도 JSON만
 * 답하라고 명시한다. 둘 중 하나가 동작하지 않아도 나머지가 받쳐 준다.
 *
 * <p>단위를 묻지 않는 것이 의도다. 저장 API가 {@code items.default_unit}과 문자열 일치를 요구해서
 * 모델이 준 단위는 쓸 수 없다. 대신 {@code numberCount}를 요구해 가격 판단 근거의 약함을 우리가
 * 판정할 수 있게 한다.
 */
final class PriceTagPrompt {

    static final String SYSTEM = """
            너는 한국 농산물 가격표 사진에서 값을 읽는 도구다.
            반드시 아래 스키마를 만족하는 JSON 객체 하나만 출력한다. 설명, 코드펜스, 여는 말을 쓰지 않는다.

            {
              "itemName": string|null,       // 사진의 농산물 이름. 한국어. 확실하지 않으면 null
              "itemConfidence": number|null, // 0~1
              "price": integer|null,         // 판매 가격의 숫자만. 통화기호와 콤마 제외
              "amount": number|null,         // 수량 또는 중량의 숫자. 사진에 근거가 없으면 null
              "amountConfidence": number|null,
              "numberCount": integer         // 가격표에서 발견한 숫자의 총 개수
            }

            규칙:
            - 사진에서 확인할 수 없는 값은 추측하지 말고 null로 둔다.
            - amount는 선택값이다. "1망", "2개"처럼 수량이 적혀 있을 때만 채운다.
            - 단위(kg, 개, 망 등)는 출력하지 않는다. 우리 시스템이 따로 결정한다.
            - numberCount에는 가격이 아닌 숫자도 포함한다. 예: "250원 / 1인 10개 제한"이면 250, 1, 10 → 3.
            - 통화는 항상 원(KRW)으로 가정한다.
            """;

    static final String USER = "이 사진의 가격표를 읽고 스키마대로 JSON만 출력해라.";

    private PriceTagPrompt() {
    }
}
