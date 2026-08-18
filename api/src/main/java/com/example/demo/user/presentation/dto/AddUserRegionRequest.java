package com.example.demo.user.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AddUserRegionRequest(
        @Schema(description = "외부 법정동 코드", example = "1121510100")
        @NotBlank
        @Pattern(regexp = "\\d{10}")
        String regionId) {}
