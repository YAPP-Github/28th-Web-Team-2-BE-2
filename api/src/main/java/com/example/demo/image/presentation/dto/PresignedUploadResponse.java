package com.example.demo.image.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

public record PresignedUploadResponse(
        @Schema(description = "이 URL로 직접 PUT한다. 만료된다")
        String uploadUrl,
        @Schema(description = "업로드 성공 후 제보에 저장할 영구 URL")
        String imageUrl,
        @Schema(description = "항상 PUT", example = "PUT")
        String method,
        @Schema(description = "uploadUrl 만료 시각. 발급 시점부터 10분")
        Instant expiresAt,
        @Schema(
                description = "PUT 요청에 같은 값으로 실어야 하는 Content-Type",
                example = "image/jpeg")
        String contentType) {}
