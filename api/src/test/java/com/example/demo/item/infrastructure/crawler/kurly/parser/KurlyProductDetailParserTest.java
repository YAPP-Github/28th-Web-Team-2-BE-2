package com.example.demo.item.infrastructure.crawler.kurly.parser;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class KurlyProductDetailParserTest {

    private final KurlyProductDetailParser parser = new KurlyProductDetailParser();

    @Test
    void extractsPricePer100GramsFromDetailPage() {
        final BigDecimal price = parser.parsePricePer100g("""
                <ul>
                  <li><dt>판매자</dt><dd>컬리</dd></li>
                  <li><dt>단위 당 가격</dt><dd>100g 당 399원</dd></li>
                  <li><dt>중량/용량</dt><dd>1kg</dd></li>
                </ul>
                """);

        assertThat(price).isEqualByComparingTo("399");
    }

    @Test
    void returnsEmptyWhenUnitPriceIsMissing() {
        assertThat(parser.parsePricePer100g("<html><body>가격 정보 없음</body></html>"))
                .isNull();
    }

    @Test
    void extractsDeliveryNoteFromDetailPage() {
        assertThat(parser.parseDeliveryNote("<div><span>샛별배송</span></div>"))
                .isEqualTo("샛별배송");
    }
}
