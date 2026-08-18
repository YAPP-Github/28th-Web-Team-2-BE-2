package com.example.demo.item.domain;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

class ItemTest {

    @Test
    void category가_없으면_생성할_수_없다() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Item("감자", "1kg", null, null))
                .withMessage("item category must not be null");
    }
}
