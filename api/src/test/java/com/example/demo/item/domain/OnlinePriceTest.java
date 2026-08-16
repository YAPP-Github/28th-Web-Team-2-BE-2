package com.example.demo.item.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class OnlinePriceTest {

    @Test
    void 온라인_가격의_저장_필드를_생성한다() {
        final OnlinePrice price = new OnlinePrice(
                1L,
                2,
                "감자",
                "햇 감자 1kg",
                890,
                100,
                "https://example.com/product",
                "무료배송",
                LocalDate.of(2026, 8, 15));

        assertThat(price.itemId()).isEqualTo(1L);
        assertThat(price.channelId()).isEqualTo(2);
        assertThat(price.itemName()).isEqualTo("감자");
        assertThat(price.productName()).isEqualTo("햇 감자 1kg");
        assertThat(price.price()).isEqualTo(890);
        assertThat(price.unit()).isEqualTo(100);
        assertThat(price.productUrl()).isEqualTo("https://example.com/product");
        assertThat(price.deliveryNote()).isEqualTo("무료배송");
        assertThat(price.createdAt()).isEqualTo(LocalDate.of(2026, 8, 15));
    }

    @Test
    void 필수_필드가_없으면_온라인_가격을_생성할_수_없다() {
        assertThatThrownBy(() -> new OnlinePrice(
                null, 2, "감자", "햇 감자", 890, 100, null, null, LocalDate.now()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
