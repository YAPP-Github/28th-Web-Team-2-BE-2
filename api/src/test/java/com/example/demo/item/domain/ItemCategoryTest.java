package com.example.demo.item.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ItemCategoryTest {

    @Test
    void API_code와_표시명_노출_순서를_고정한다() {
        assertThat(ItemCategory.values()).containsExactly(
                ItemCategory.ROOT_VEGETABLES,
                ItemCategory.LEAFY_GREENS,
                ItemCategory.FRUITING_VEGETABLES,
                ItemCategory.PEPPERS,
                ItemCategory.SEASONINGS,
                ItemCategory.MUSHROOMS,
                ItemCategory.FRUITS);
        assertThat(ItemCategory.ROOT_VEGETABLES.code()).isEqualTo("ROOT_VEGETABLES");
        assertThat(ItemCategory.ROOT_VEGETABLES.displayName()).isEqualTo("감자·뿌리");
        assertThat(ItemCategory.LEAFY_GREENS.displayName()).isEqualTo("잎채소");
        assertThat(ItemCategory.FRUITING_VEGETABLES.displayName()).isEqualTo("열매채소");
        assertThat(ItemCategory.PEPPERS.displayName()).isEqualTo("고추");
        assertThat(ItemCategory.SEASONINGS.displayName()).isEqualTo("양념");
        assertThat(ItemCategory.MUSHROOMS.displayName()).isEqualTo("버섯");
        assertThat(ItemCategory.FRUITS.displayName()).isEqualTo("과채");
    }
}
