package com.example.demo.auth.infrastructure.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class RedisRefreshTokenStoreTest {

    @Test
    void refresh_token을_해시_키로_저장하고_GETDEL로_소비한다() {
        final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        final ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(values);
        when(values.getAndDelete("auth:refresh:hash")).thenReturn("subject");
        final RedisRefreshTokenStore store = new RedisRefreshTokenStore(redisTemplate);

        store.save("hash", "subject", Duration.ofDays(14));

        assertThat(store.consume("hash", "subject")).isTrue();
        assertThat(store.consume("hash", "other-subject")).isFalse();
        verify(values).set("auth:refresh:hash", "subject", Duration.ofDays(14));
    }

    @Test
    void 로그아웃은_해시_키를_삭제한다() {
        final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        final RedisRefreshTokenStore store = new RedisRefreshTokenStore(redisTemplate);

        store.delete("hash");

        verify(redisTemplate).delete("auth:refresh:hash");
    }
}
