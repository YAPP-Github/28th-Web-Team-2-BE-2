package com.example.demo.price.domain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.demo.price.application.command.CollectionTask;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class PriceDomainValidationTest {

    @Test
    void 양과_단위가_유효해야_한다() {
        assertThatThrownBy(() -> new ParsedQuantity(BigDecimal.ZERO, PriceUnit.KG))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ParsedQuantity(BigDecimal.ONE, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 상품_제안의_가격과_상품명은_유효해야_한다() {
        assertThatThrownBy(() -> new RawOffer(
                "p", " ", "url", BigDecimal.ONE, BigDecimal.ZERO, null, null, true, false))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RawOffer(
                "p", "감자", "url", BigDecimal.valueOf(-1), BigDecimal.ZERO,
                null, null, true, false)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 수집_task의_필수_값을_검증한다() {
        assertThatThrownBy(() -> new CollectionTask(
                0L, "감자", ChannelCode.OASIS, PriceUnit.KG, LocalDate.now(), 1L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CollectionTask(
                1L, " ", ChannelCode.OASIS, PriceUnit.KG, LocalDate.now(), 1L))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
