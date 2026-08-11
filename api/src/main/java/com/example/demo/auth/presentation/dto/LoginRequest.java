package com.example.demo.auth.presentation.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@NotBlank(message = "OAuth idToken은 필수입니다.") String idToken) {}
