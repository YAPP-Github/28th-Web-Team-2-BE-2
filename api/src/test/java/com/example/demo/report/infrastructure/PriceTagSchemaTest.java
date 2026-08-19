package com.example.demo.report.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.report.application.contract.ExtractedPriceTag;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 프롬프트가 요구하는 키와 파서가 읽는 키가 갈라지지 않는지 본다.
 *
 * <p>이전에는 두 곳이 각자 리터럴을 들고 있어, 한쪽만 바꾸면 파서가 모든 필드를 null 로 읽고
 * 예외 없이 200 을 반환했다. 실패하는 테스트가 하나도 없었다.
 */
class PriceTagSchemaTest {

    private static final List<String> KEYS = List.of(
            PriceTagSchema.ITEM_NAME,
            PriceTagSchema.ITEM_CONFIDENCE,
            PriceTagSchema.PRICE,
            PriceTagSchema.PRICE_CONFIDENCE,
            PriceTagSchema.PRICE_BASIS,
            PriceTagSchema.AMOUNT,
            PriceTagSchema.AMOUNT_CONFIDENCE,
            PriceTagSchema.OTHER_NUMBER_COUNT);

    @Test
    void 프롬프트가_파서가_읽는_모든_키를_요구한다() {
        assertThat(PriceTagPrompt.SYSTEM).contains(KEYS);
    }

    // 프롬프트가 지시한 스키마대로 답하면 모든 필드가 채워져야 한다.
    @Test
    void 스키마를_그대로_채운_응답은_모든_필드가_매핑된다() {
        final String json = """
                {"%s":"오이","%s":0.96,"%s":250,"%s":0.90,"%s":"1개","%s":1,"%s":0.72,"%s":0}"""
                .formatted(KEYS.toArray());

        final ExtractedPriceTag result = new PriceTagResponseParser(new ObjectMapper()).parse(json);

        assertThat(result.itemName()).isEqualTo("오이");
        assertThat(result.itemConfidence()).isNotNull();
        assertThat(result.price()).isEqualTo(250);
        assertThat(result.priceConfidence()).isNotNull();
        assertThat(result.priceBasis()).isEqualTo("1개");
        assertThat(result.amount()).isEqualByComparingTo("1");
        assertThat(result.amountConfidence()).isNotNull();
        assertThat(result.otherNumberCount()).isZero();
    }
}
