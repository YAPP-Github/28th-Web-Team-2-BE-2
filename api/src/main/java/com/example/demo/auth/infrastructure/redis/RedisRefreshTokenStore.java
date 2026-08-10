package com.example.demo.auth.infrastructure.redis;

import com.example.demo.auth.application.port.RefreshTokenStore;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RedisRefreshTokenStore implements RefreshTokenStore {

    private static final String KEY_PREFIX = "auth:refresh:";
    private static final RedisScript<Long> CONSUME_SCRIPT = new DefaultRedisScript<>(
            """
                    local current = redis.call('GET', KEYS[1])
                    if current == ARGV[1] then
                        return redis.call('DEL', KEYS[1])
                    end
                    return 0
                    """,
            Long.class);

    private final StringRedisTemplate redisTemplate;

    @Override
    public void save(final Long userId, final String tokenHash, final Duration ttl) {
        redisTemplate.opsForValue().set(keyOf(userId), tokenHash, ttl);
    }

    @Override
    public boolean consume(final Long userId, final String tokenHash) {
        final Long deleted = redisTemplate.execute(CONSUME_SCRIPT, List.of(keyOf(userId)), tokenHash);
        return Long.valueOf(1).equals(deleted);
    }

    @Override
    public void delete(final Long userId) {
        redisTemplate.delete(keyOf(userId));
    }

    private String keyOf(final Long userId) {
        return KEY_PREFIX + userId;
    }
}
