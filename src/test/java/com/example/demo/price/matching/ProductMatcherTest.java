package com.example.demo.price.domain.matching;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.price.domain.ParsedQuantity;
import com.example.demo.price.domain.PriceUnit;
import com.example.demo.price.domain.RawOffer;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ProductMatcherTest {

    private final ProductMatcher matcher = new ProductMatcher();

    @Test
    void 표준_품목은_매칭하고_가공상품과_광고는_제외한다() {
        assertThat(matcher.matches("감자", offer("국내산 감자 1kg", false))).isTrue();
        assertThat(matcher.matches("감자", offer("감자칩 1kg", false))).isFalse();
        assertThat(matcher.matches("감자", offer("감자 1kg", true))).isFalse();
        assertThat(matcher.matches("고구마", offer("감자 1kg", false))).isFalse();
    }

    private RawOffer offer(final String title, final boolean advertisement) {
        return new RawOffer(
                "p-1", title, "https://example.com/p-1", BigDecimal.valueOf(1000),
                BigDecimal.ZERO, new ParsedQuantity(BigDecimal.ONE, PriceUnit.KG), "국내산", true,
                advertisement);
    }
}
