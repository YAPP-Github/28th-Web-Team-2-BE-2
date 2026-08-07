package com.example.demo.sample.domain;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class SampleMessageTest {

    @Test
    void blank_message는_생성할_수_없다() {
        assertThrows(IllegalArgumentException.class, () -> new SampleMessage(" "));
    }
}
