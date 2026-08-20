package com.example.demo.item.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ItemUnitTest {

    private static final int PER_100_GRAMS = 100;

    @Test
    @DisplayName("kg 단위는 100g 기준 가격을 킬로그램 기준으로 환산한다")
    void convertsKilogram() {
        assertThat(ItemUnit.of("1kg").convert(320, PER_100_GRAMS)).isEqualTo(3200);
    }

    @Test
    @DisplayName("100g 단위는 그대로 둔다")
    void keepsHundredGrams() {
        assertThat(ItemUnit.of("100g").convert(320, PER_100_GRAMS)).isEqualTo(320);
    }

    @Test
    @DisplayName("무게가 아닌 단위는 환산할 수 없다")
    void cannotConvertNonWeightUnit() {
        assertThat(ItemUnit.of("1개").convertible()).isFalse();
        assertThat(ItemUnit.of("1포기").convertible()).isFalse();
        assertThat(ItemUnit.of("1개").convert(320, PER_100_GRAMS)).isNull();
    }

    @Test
    @DisplayName("단위가 없으면 환산할 수 없고 라벨도 없다")
    void handlesMissingUnit() {
        assertThat(ItemUnit.of(null).convertible()).isFalse();
        assertThat(ItemUnit.of(null).label()).isNull();
    }

    @Test
    @DisplayName("환산 결과는 반올림한다")
    void roundsResult() {
        // 333원/100g -> 1kg 이면 3330원
        assertThat(ItemUnit.of("1kg").convert(333, PER_100_GRAMS)).isEqualTo(3330);
        // 5원/100g -> 250g 이면 12.5 -> 13원
        assertThat(ItemUnit.of("250g").convert(5, PER_100_GRAMS)).isEqualTo(13);
    }
}
