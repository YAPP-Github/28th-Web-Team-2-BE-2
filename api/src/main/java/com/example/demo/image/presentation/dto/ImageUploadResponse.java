package com.example.demo.image.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record ImageUploadResponse(
        @Schema(
                description = "제보 저장 시 photoUrl로 사용할 영구 URL",
                example = "https://marketgo-images.s3.ap-northeast-2.amazonaws.com/images/"
                        + "3f2504e0-4f89-11d3-9a0c-0305e82c3301.jpg")
        String imageUrl) {}
