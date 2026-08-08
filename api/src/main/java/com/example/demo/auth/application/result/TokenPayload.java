package com.example.demo.auth.application.result;

import java.time.Instant;

public record TokenPayload(Long userId, Instant expiresAt) {}
