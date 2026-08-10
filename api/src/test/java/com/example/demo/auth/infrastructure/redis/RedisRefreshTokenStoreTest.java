package com.example.demo.auth.infrastructure.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

class RedisRefreshTokenStoreTest {

    @Test
    void 사용자별_키에_refresh_token_해시를_저장하고_한_번만_소비한다() {
        final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        final ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(values);
        when(redisTemplate.execute(any(RedisScript.class), eq(List.of("auth:refresh:1")), eq("hash")))
                .thenReturn(1L, 0L);
        final RedisRefreshTokenStore store = new RedisRefreshTokenStore(redisTemplate);

        store.save(1L, "hash", Duration.ofDays(14));

        assertThat(store.consume(1L, "hash")).isTrue();
        assertThat(store.consume(1L, "hash")).isFalse();
        verify(values).set("auth:refresh:1", "hash", Duration.ofDays(14));
    }

    @Test
    void 일치하지_않는_refresh_token은_세션을_소비하지_않는다() {
        final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        when(redisTemplate.execute(any(RedisScript.class), eq(List.of("auth:refresh:1")), eq("other-hash")))
                .thenReturn(0L);
        final RedisRefreshTokenStore store = new RedisRefreshTokenStore(redisTemplate);

        assertThat(store.consume(1L, "other-hash")).isFalse();
    }

    @Test
    void 로그아웃은_사용자별_Redis_키를_삭제한다() {
        final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        final RedisRefreshTokenStore store = new RedisRefreshTokenStore(redisTemplate);

        store.delete(1L);

        verify(redisTemplate).delete("auth:refresh:1");
    }
}
