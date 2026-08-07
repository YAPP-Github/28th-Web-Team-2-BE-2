package com.example.demo.kamis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

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

    @Test
    void 외부_클라이언트_모듈의_공개_계약을_제공한다() {
        assertThatCode(() -> Class.forName("com.example.demo.external.kamis.feign.KamisClient"))
                .doesNotThrowAnyException();
    }
}
