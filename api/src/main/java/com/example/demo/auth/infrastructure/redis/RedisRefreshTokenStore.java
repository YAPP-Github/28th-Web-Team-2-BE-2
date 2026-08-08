package com.example.demo.auth.infrastructure.redis;

import com.example.demo.auth.application.port.RefreshTokenStore;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RedisRefreshTokenStore implements RefreshTokenStore {

    private static final String KEY_PREFIX = "auth:refresh:";

    private final StringRedisTemplate redisTemplate;

    @Override
    public void save(final Long userId, final String tokenHash, final Duration ttl) {
        redisTemplate.opsForValue().set(keyOf(userId), tokenHash, ttl);
    }

    @Override
    public boolean matches(final Long userId, final String tokenHash) {
        final String savedTokenHash = redisTemplate.opsForValue().get(keyOf(userId));
        return tokenHash.equals(savedTokenHash);
    }

    @Override
    public void delete(final Long userId) {
        redisTemplate.delete(keyOf(userId));
    }

    private String keyOf(final Long userId) {
        return KEY_PREFIX + userId;
    }
}
