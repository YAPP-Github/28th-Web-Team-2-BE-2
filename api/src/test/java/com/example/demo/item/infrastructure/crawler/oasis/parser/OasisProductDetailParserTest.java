package com.example.demo.item.infrastructure.crawler.oasis.parser;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class OasisProductDetailParserTest {

    private final OasisProductDetailParser parser = new OasisProductDetailParser();

    @Test
    void convertsTenGramPriceToOneHundredGramPrice() {
        final BigDecimal price = parser.parsePricePer100g("""
                <div class="product_option">
                  <span class="opt_name">100g x 1개</span>
                  <span class="opt_price">2,900원</span>
                  <span class="opt_unit">10g당 290원</span>
                </div>
                """);

        assertThat(price).isEqualByComparingTo("2900");
    }

    @Test
    void extractsDeliveryNote() {
        assertThat(parser.parseDeliveryNote("<span class=\"badge_deliveryOasis\">오아시스배송</span>"))
                .isEqualTo("오아시스배송");
    }

    @Test
    void returnsEmptyWhenUnitPriceIsMissing() {
        assertThat(parser.parsePricePer100g("<div class=\"product_option\">상품 정보</div>")).isNull();
    }
}
