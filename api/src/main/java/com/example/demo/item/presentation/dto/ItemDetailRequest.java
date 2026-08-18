package com.example.demo.item.presentation.dto;

import jakarta.validation.constraints.NotBlank;

public record ItemDetailRequest(@NotBlank String regionId) {}
