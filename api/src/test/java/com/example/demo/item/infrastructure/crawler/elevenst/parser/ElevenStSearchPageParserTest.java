package com.example.demo.item.infrastructure.crawler.elevenst.parser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.demo.item.infrastructure.crawler.elevenst.ElevenStProduct;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;

class ElevenStSearchPageParserTest {

    private final ElevenStSearchPageParser parser = new ElevenStSearchPageParser(new ObjectMapper());

    @Test
    void extractsProductFieldsFromSearchResponse() {
        final List<ElevenStProduct> products = parser.parse(searchJson());

        assertThat(products).singleElement().satisfies(product -> {
            assertThat(product.externalProductId()).isEqualTo("879232236");
            assertThat(product.name()).isEqualTo("햇감자 5kg");
            assertThat(product.sellingPrice()).isEqualByComparingTo("8900");
            assertThat(product.originalPrice()).isEqualByComparingTo("9900");
            assertThat(product.productUrl()).isEqualTo(URI.create("https://www.11st.co.kr/products/879232236"));
            assertThat(product.deliveryNote()).isEqualTo("무료");
        });
    }

    @Test
    void throwsForInvalidResponse() {
        assertThatThrownBy(() -> parser.parse("not-json"))
                .isInstanceOfSatisfying(IllegalStateException.class, exception ->
                        assertThat(exception.getCause()).isNull());
    }

    @Test
    void extractsProductsFromBrowserJsonDocument() {
        assertThat(parser.parse("<html><body><pre>" + searchJson() + "</pre></body></html>"))
                .hasSize(1);
    }

    private String searchJson() {
        return """
                {"data":[{"items":[{"id":"879232236","title":"햇감자 5kg","finalPrc":8900,"deliveryDescription":"무료","maxDiscountInfo":{"sellPrice":9900}}]}]}
                """;
    }
}
