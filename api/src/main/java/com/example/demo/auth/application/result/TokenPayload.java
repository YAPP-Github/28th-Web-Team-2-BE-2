package com.example.demo.auth.application.result;

import java.time.Instant;

public record TokenPayload(String subject, Instant expiresAt) {}
