package com.example.demo.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.data.auditing.AuditingHandler;

@SpringBootTest
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class PersistenceConfigTest {

    private final ApplicationContext applicationContext;

    @Test
    void JPA_감사와_Redis_문자열_template과_Querydsl_factory를_등록한다() {
        assertThat(applicationContext.getBeansOfType(AuditingHandler.class)).isNotEmpty();
        assertThat(applicationContext.getBean("stringRedisTemplate")).isNotNull();
        assertThat(applicationContext.containsBean("jpaQueryFactory")).isTrue();
    }
}
