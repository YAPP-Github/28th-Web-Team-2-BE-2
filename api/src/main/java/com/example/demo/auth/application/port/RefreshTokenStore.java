package com.example.demo.auth.application.port;

import java.time.Duration;

public interface RefreshTokenStore {

    void save(Long userId, String tokenHash, Duration ttl);

    boolean matches(Long userId, String tokenHash);

    void delete(Long userId);
}
