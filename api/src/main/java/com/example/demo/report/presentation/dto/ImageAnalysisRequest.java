package com.example.demo.report.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * 인식 요청.
 *
 * <p>multipart로 이미지를 다시 받지 않고 업로드 API가 돌려준 {@code imageUrl}을 받는다. 사용자가
 * 사진을 한 번만 올리면 인식과 제보 저장이 같은 이미지를 쓸 수 있다. multipart로 받으면 인식용과
 * 저장용으로 두 번 올려야 하거나 제보에 사진이 남지 않는다.
 */
public record ImageAnalysisRequest(
        @NotBlank @Size(max = 500)
        @Schema(
                description = "업로드 API(POST /api/v1/images)가 돌려준 영구 URL",
                example = "https://marketgo-images.s3.ap-northeast-2.amazonaws.com/images/"
                        + "3f2504e0-4f89-11d3-9a0c-0305e82c3301.jpg")
        String imageUrl,
        @Positive
        @Schema(
                description = "사용자가 이미 품목을 골랐다면 그 ID. 주면 인식 결과보다 우선한다",
                example = "12",
                nullable = true)
        Long itemId) {}
