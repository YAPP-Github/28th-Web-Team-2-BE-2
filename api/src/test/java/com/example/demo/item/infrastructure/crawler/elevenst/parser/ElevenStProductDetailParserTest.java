package com.example.demo.item.infrastructure.crawler.elevenst.parser;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ElevenStProductDetailParserTest {

    private final ElevenStProductDetailParser parser = new ElevenStProductDetailParser();

    @Test
    void convertsKilogramUnitPriceToOneHundredGrams() {
        assertThat(parser.parsePricePer100g("<ul id='pricePerUnitResult'><dd>1kg당 8,900원</dd></ul>"))
                .isEqualByComparingTo(BigDecimal.valueOf(890));
    }

    @Test
    void extractsDeliveryNote() {
        assertThat(parser.parseDeliveryNote("<div class='delivery'>무료배송 CJ대한통운</div>"))
                .isEqualTo("무료배송 CJ대한통운");
    }

    @Test
    void fallsBackToProductUnitWhenDetailUnitPriceIsNotRendered() {
        assertThat(parser.parsePricePer100g(
                "<title>햇감자 5kg</title>", "햇감자 5kg", BigDecimal.valueOf(8900)))
                .isEqualByComparingTo("178");
    }
}
