package com.example.demo.auth.application.port;

import java.time.Duration;

public interface RefreshTokenStore {

    void save(String tokenHash, String subject, Duration ttl);

    boolean consume(String tokenHash, String subject);

    void delete(String tokenHash);
}
