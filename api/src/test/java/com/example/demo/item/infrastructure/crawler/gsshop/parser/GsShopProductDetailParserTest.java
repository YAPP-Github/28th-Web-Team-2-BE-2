package com.example.demo.item.infrastructure.crawler.gsshop.parser;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class GsShopProductDetailParserTest {

    private final GsShopProductDetailParser parser = new GsShopProductDetailParser();

    @Test
    void convertsKilogramUnitPriceToOneHundredGrams() {
        assertThat(parser.parsePricePer100g("<div class='unit-price'>1kg당 8,900원</div>"))
                .isEqualByComparingTo("890");
    }

    @Test
    void fallsBackToProductUnitWhenDetailUnitPriceIsMissing() {
        assertThat(parser.parsePricePer100g(
                "<title>햇감자 5kg</title>", "햇감자 5kg", BigDecimal.valueOf(8900)))
                .isEqualByComparingTo("178");
    }

    @Test
    void returnsNullWhenProductNameIsMissingForFallback() {
        assertThat(parser.parsePricePer100g("<html></html>", null, BigDecimal.valueOf(8900)))
                .isNull();
    }

    @Test
    void extractsDeliveryNote() {
        assertThat(parser.parseDeliveryNote("<div class='delivery'>무료배송</div>"))
                .isEqualTo("무료배송");
    }
}
