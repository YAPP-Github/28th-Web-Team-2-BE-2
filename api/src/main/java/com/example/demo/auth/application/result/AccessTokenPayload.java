package com.example.demo.auth.application.result;

import com.example.demo.auth.domain.UserRole;

public record AccessTokenPayload(Long userId, UserRole role) {}
