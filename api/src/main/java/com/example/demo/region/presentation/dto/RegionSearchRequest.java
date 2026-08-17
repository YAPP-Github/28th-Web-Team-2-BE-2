package com.example.demo.region.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegionSearchRequest(
        @Schema(description = "검색할 동 이름", example = "성성동")
        @NotBlank
        @Size(min = 2, max = 20)
        @Pattern(regexp = "^[가-힣]+(?: [가-힣]+)*$")
        String keyword) {}
