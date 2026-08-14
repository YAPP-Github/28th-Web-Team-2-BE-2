package com.example.demo.item.infrastructure.crawler.kurly.parser;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.item.infrastructure.crawler.kurly.KurlyProduct;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;

class KurlySearchPageParserTest {

    private final KurlySearchPageParser parser = new KurlySearchPageParser();

    @Test
    void extractsProductIdNamePricesAndUrlFromSearchResults() {
        final List<KurlyProduct> products = parser.parse(searchHtml());

        assertThat(products).hasSize(3);
        assertThat(products.get(0).externalProductId()).isEqualTo("5026448");
        assertThat(products.get(0).name()).isEqualTo("[KF365] 감자 1kg");
        assertThat(products.get(0).sellingPrice()).isEqualByComparingTo("2990");
        assertThat(products.get(0).originalPrice()).isEqualByComparingTo("4990");
        assertThat(products.get(0).productUrl()).isEqualTo(URI.create("https://www.kurly.com/goods/5026448"));
    }

    @Test
    void excludesCardsWithoutPriceOrProductName() {
        final String html = searchHtml()
                .replace("<span class=\"product-name\">감자 샐러드</span>", "<span class=\"product-name\"></span>");

        assertThat(parser.parse(html)).hasSize(2);
    }

    @Test
    void returnsEmptyResultForBlankPage() {
        assertThat(parser.parse(" ")).isEmpty();
    }

    @Test
    void extractsProductNameFromCurrentKurlyGeneratedMarkup() {
        final List<KurlyProduct> products = parser.parse("""
                <a href="/goods/5026448">
                  <div class="product-details">
                    <span class="delivery">샛별배송</span>
                    <span class="generated-product-name">골든킹 감자 900g</span>
                    <p>분이 폴폴 날리는 신품종 감자</p>
                    <div class="discount-price">
                      <span class="dimmed-price"><span class="price-number">4,990</span></span>
                      <span class="sales-price"><span class="price-number">3,990</span></span>
                    </div>
                  </div>
                </a>
                """);

        assertThat(products).singleElement().satisfies(product -> {
            assertThat(product.name()).isEqualTo("골든킹 감자 900g");
            assertThat(product.deliveryNote()).isEqualTo("샛별배송");
            assertThat(product.sellingPrice()).isEqualByComparingTo("3990");
            assertThat(product.originalPrice()).isEqualByComparingTo("4990");
        });
    }

    private String searchHtml() {
        return """
                <html><body>
                <a class="product-card" href="/goods/5026448">
                  <span class="product-name">[KF365] 감자 1kg</span>
                  <div class="discount-price">
                    <span class="dimmed-price"><span class="price-number">4,990</span>원</span>
                    <span class="sales-price"><span class="price-number">2,990</span>원</span>
                  </div>
                </a>
                <a class="product-card" href="/goods/5049249">
                  <span class="product-name">[팜송] 한끼 감자 300g</span>
                  <span class="sales-price"><span class="price-number">2,490</span>원</span>
                </a>
                <a class="product-card" href="/goods/5153165">
                  <span class="product-name">감자 샐러드</span>
                  <span class="sales-price"><span class="price-number">4,600</span>원</span>
                </a>
                </body></html>
                """;
    }
}
