package com.example.demo.item.infrastructure.crawler.oasis.parser;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.item.infrastructure.crawler.oasis.OasisProduct;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;

class OasisSearchPageParserTest {

    private final OasisSearchPageParser parser = new OasisSearchPageParser();

    @Test
    void extractsProductFieldsFromSearchCard() {
        final List<OasisProduct> products = parser.parse(searchHtml());

        assertThat(products).singleElement().satisfies(product -> {
            assertThat(product.externalProductId()).isEqualTo("59370");
            assertThat(product.name()).isEqualTo("GAP 말랑촉촉 청도 감자 (1kg)");
            assertThat(product.sellingPrice()).isEqualByComparingTo("2900");
            assertThat(product.originalPrice()).isEqualByComparingTo("3300");
            assertThat(product.productUrl()).isEqualTo(URI.create("https://www.oasis.co.kr/product/detail/59370?categoryId="));
            assertThat(product.deliveryNote()).isEqualTo("오아시스배송");
        });
    }

    @Test
    void returnsEmptyForBlankPage() {
        assertThat(parser.parse(" ")).isEmpty();
    }

    private String searchHtml() {
        return """
                <div class="wrapBox">
                  <div class="wrapImg">
                    <a href="/product/detail/59370?categoryId=">
                      <img alt="GAP 말랑촉촉 청도 감자 대표이미지 섬네일">
                    </a>
                  </div>
                  <div class="wrapInfo">
                    <div class="info_title">
                      <a class="listTit" href="/product/detail/59370?categoryId=">GAP 말랑촉촉 청도 감자 (1kg)</a>
                    </div>
                    <div class="info_price">
                      <span class="price_discount"><b>2,900</b>원</span>
                      <span class="price_original"><b>3,300</b>원</span>
                    </div>
                    <div class="optionLowestBox"><div class="info_option">100g당 290원</div></div>
                    <div class="info_badges btm"><span class="badge_deliveryOasis">오아시스배송</span></div>
                  </div>
                </div>
                """;
    }
}
