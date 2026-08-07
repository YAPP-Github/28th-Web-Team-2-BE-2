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
    void 사용자별_키에_refresh_token_해시를_저장하고_비교한다() {
        final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        final ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(values);
        when(values.get("auth:refresh:1")).thenReturn("hash");
        final RedisRefreshTokenStore store = new RedisRefreshTokenStore(redisTemplate);

        store.save(1L, "hash", Duration.ofDays(14));

        assertThat(store.matches(1L, "hash")).isTrue();
        assertThat(store.matches(1L, "other-hash")).isFalse();
        verify(values).set("auth:refresh:1", "hash", Duration.ofDays(14));
    }

    @Test
    void 로그아웃은_사용자별_Redis_키를_삭제한다() {
        final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        final RedisRefreshTokenStore store = new RedisRefreshTokenStore(redisTemplate);

        store.delete(1L);

        verify(redisTemplate).delete("auth:refresh:1");
    }
}
