package com.example.demo.price.domain.normalization;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.price.domain.PriceUnit;
import org.junit.jupiter.api.Test;

class QuantityParserTest {

    private final QuantityParser quantityParser = new QuantityParser();

    @Test
    void 중량을_킬로그램으로_파싱한다() {
        assertThat(quantityParser.parse("국내산 감자 1kg").orElseThrow().unit())
                .isEqualTo(PriceUnit.KG);
    }

    @Test
    void 중량과_개수를_각각_파싱한다() {
        assertThat(quantityParser.parse("500g").orElseThrow().value()).hasToString("500");
        assertThat(quantityParser.parse("10개입").orElseThrow().unit())
                .isEqualTo(PriceUnit.COUNT);
    }

    @Test
    void 비어_있거나_알_수_없는_수량은_파싱하지_않는다() {
        assertThat(quantityParser.parse(" ")).isEmpty();
        assertThat(quantityParser.parse("중량 미표기")).isEmpty();
    }
}
