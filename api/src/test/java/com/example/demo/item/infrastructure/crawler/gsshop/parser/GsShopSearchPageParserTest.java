package com.example.demo.item.infrastructure.crawler.gsshop.parser;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.item.infrastructure.crawler.gsshop.GsShopProduct;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;

class GsShopSearchPageParserTest {

    private final GsShopSearchPageParser parser = new GsShopSearchPageParser();

    @Test
    void extractsProductFieldsFromSearchCard() {
        final List<GsShopProduct> products = parser.parse(searchHtml());

        assertThat(products).singleElement().satisfies(product -> {
            assertThat(product.externalProductId()).isEqualTo("57668979");
            assertThat(product.name()).isEqualTo("[한정특가] 포슬포슬 26년 햇 감자 5kg 대");
            assertThat(product.sellingPrice()).isEqualByComparingTo("8900");
            assertThat(product.productUrl()).isEqualTo(URI.create("https://www.gsshop.com/prd/prd.gs?prdid=57668979"));
        });
    }

    @Test
    void returnsEmptyForBlankPage() {
        assertThat(parser.parse(" ")).isEmpty();
    }

    @Test
    void excludesCardWithoutSellingPrice() {
        assertThat(parser.parse("""
                <a class="prd-item" data-prdid="1" href="/prd/prd.gs?prdid=1">
                  <dl class="prd-info"><dt class="prd-name">감자</dt></dl>
                </a>
                """)).isEmpty();
    }

    private String searchHtml() {
        return """
                <section class="prd-list">
                  <ul>
                    <li>
                      <a class="prd-item" data-prdid="57668979"
                         href="https://www.gsshop.com/prd/prd.gs?prdid=57668979&kwd=%EA%B0%90%EC%9E%90">
                        <dl class="prd-info">
                          <dt class="prd-name">[한정특가] 포슬포슬 26년 햇 감자 5kg 대</dt>
                          <dd class="price-info"><span class="price"><span class="set-price"><strong>8,900</strong>원</span></span></dd>
                        </dl>
                      </a>
                    </li>
                  </ul>
                </section>
                """;
    }
}
