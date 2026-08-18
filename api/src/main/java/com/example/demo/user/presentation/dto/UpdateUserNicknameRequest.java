package com.example.demo.user.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUserNicknameRequest(
        @Schema(description = "닉네임", example = "장보고01")
        @NotBlank
        @Size(min = 2, max = 10)
        @Pattern(regexp = "^[가-힣A-Za-z0-9]+$")
        String nickname) {

    public UpdateUserNicknameRequest {
        if (nickname != null) {
            nickname = nickname.strip();
        }
    }
}
