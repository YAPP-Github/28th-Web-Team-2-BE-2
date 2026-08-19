package com.example.demo.report.infrastructure;

/**
 * 모델과 주고받는 추출 스키마의 키 이름.
 *
 * <p>프롬프트 산문과 파서가 각자 문자열 리터럴을 들고 있으면 한쪽만 바꿔도 아무 신호가 없다 —
 * 파서는 모든 필드를 {@code null}로 읽고 예외 없이 200 을 반환한다. 두 곳이 같은 상수를 쓰게 해
 * 이름이 갈라질 수 없게 한다.
 */
final class PriceTagSchema {

    static final String ITEM_NAME = "itemName";
    static final String ITEM_CONFIDENCE = "itemConfidence";
    static final String PRICE = "price";
    static final String PRICE_CONFIDENCE = "priceConfidence";
    static final String PRICE_BASIS = "priceBasis";
    static final String AMOUNT = "amount";
    static final String AMOUNT_CONFIDENCE = "amountConfidence";
    static final String OTHER_NUMBER_COUNT = "otherNumberCount";

    /** 프롬프트에 박아 넣을 스키마 블록. 키가 바뀌면 프롬프트와 파서가 함께 따라온다. */
    static final String JSON_SHAPE = """
            {
              "%s": string|null,        // 사진의 농산물 이름. 한국어. 확실하지 않으면 null
              "%s": number|null,        // 0~1
              "%s": integer|null,       // 판매 가격의 숫자만. 통화기호와 콤마 제외
              "%s": number|null,        // 0~1. 이 숫자가 판매 가격이라는 확신
              "%s": string|null,        // 그 가격이 적용되는 수량 표기. 사진에 적힌 그대로
              "%s": number|null,        // 수량 또는 중량의 숫자. 사진에 근거가 없으면 null
              "%s": number|null,        // 0~1
              "%s": integer             // 가격·수량·기준으로 쓰지 않은 나머지 숫자의 개수
            }""".formatted(
            ITEM_NAME, ITEM_CONFIDENCE, PRICE, PRICE_CONFIDENCE,
            PRICE_BASIS, AMOUNT, AMOUNT_CONFIDENCE, OTHER_NUMBER_COUNT);

    private PriceTagSchema() {
    }
}
