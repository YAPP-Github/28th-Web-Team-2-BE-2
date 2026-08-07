package com.example.demo.price.domain.normalization;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.price.domain.NormalizedPrice;
import com.example.demo.price.domain.PriceUnit;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class RepresentativePriceCalculatorTest {

    private final RepresentativePriceCalculator calculator = new RepresentativePriceCalculator();

    @Test
    void 유효_후보가_세_개_이상이면_중앙값을_반환한다() {
        final List<NormalizedPrice> prices = List.of(
                price("100"), price("300"), price("200"), price("10000"));

        assertThat(calculator.calculate(prices).orElseThrow().amount())
                .isEqualByComparingTo("250");
    }

    @Test
    void 유효_후보가_세_개_미만이면_대표값을_만들지_않는다() {
        assertThat(calculator.calculate(List.of(price("100"), price("200")))).isEmpty();
    }

    private NormalizedPrice price(final String amount) {
        return new NormalizedPrice(new BigDecimal(amount), PriceUnit.KG);
    }
}
