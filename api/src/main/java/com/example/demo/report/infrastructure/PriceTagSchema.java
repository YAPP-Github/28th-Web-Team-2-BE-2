package com.example.demo.report.infrastructure;

/**
 * 모델과 주고받는 추출 스키마의 키 이름.
 *
 * <p>프롬프트 산문과 파서가 각자 문자열 리터럴을 들고 있으면 한쪽만 바꿔도 아무 신호가 없다 —
 * 파서는 모든 필드를 {@code null}로 읽고 예외 없이 200 을 반환한다.
 *
 * <p>프롬프트 텍스트를 이 상수들로 조립하지는 않는다. 위치 인자 8개를 끼우는 템플릿은 읽기 어렵고,
 * 갈라짐은 {@code PriceTagSchemaTest}가 프롬프트에 모든 키가 있는지 검사해 이미 막는다.
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


    private PriceTagSchema() {
    }
}
