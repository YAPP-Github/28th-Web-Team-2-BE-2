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
    public void save(final String tokenHash, final String subject, final Duration ttl) {
        redisTemplate.opsForValue().set(key(tokenHash), subject, ttl);
    }

    @Override
    public boolean consume(final String tokenHash, final String subject) {
        final String savedSubject = redisTemplate.opsForValue().getAndDelete(key(tokenHash));
        return subject.equals(savedSubject);
    }

    @Override
    public void delete(final String tokenHash) {
        redisTemplate.delete(key(tokenHash));
    }

    private String key(final String tokenHash) {
        return KEY_PREFIX + tokenHash;
    }
}
