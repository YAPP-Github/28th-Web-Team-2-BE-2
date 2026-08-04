package com.example.demo.sample.presentation.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateSampleMessageRequest(@NotBlank String message) {}
