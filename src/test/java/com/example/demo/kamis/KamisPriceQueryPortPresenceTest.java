package com.example.demo.kamis;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

@SpringBootTest
class KamisPriceQueryPortPresenceTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void 일별_부류별_가격_조회_포트_빈을_제공한다() {
        assertThat(applicationContext.containsBean("kamisPriceQueryPort")).isTrue();
    }
}
