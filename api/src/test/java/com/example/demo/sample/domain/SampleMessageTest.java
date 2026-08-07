package com.example.demo.sample.domain;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class SampleMessageTest {

    @Test
    void blank_message는_생성할_수_없다() {
        assertThrows(IllegalArgumentException.class, () -> new SampleMessage(" "));
    }

    @Test
    void 이백자_메시지는_생성할_수_있다() {
        assertDoesNotThrow(() -> new SampleMessage("a".repeat(200)));
    }

    @Test
    void 이백일자_메시지는_생성할_수_없다() {
        assertThrows(IllegalArgumentException.class, () -> new SampleMessage("a".repeat(201)));
    }
}
