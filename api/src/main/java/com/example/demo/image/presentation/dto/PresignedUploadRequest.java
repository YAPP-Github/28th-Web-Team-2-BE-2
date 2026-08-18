package com.example.demo.image.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * presigned PUT 발급 요청. 계약이 정한 세 필드다.
 *
 * <p>{@code filename}은 key에 쓰지 않는다({@code ImageKey}가 UUID로 생성한다). 그래도 받는 이유는
 * 계약에 있는 필드이고, 클라이언트가 형식을 판별하는 근거로 쓰기 때문이다. 실제 형식 판별은
 * {@code contentType}으로만 한다 — 확장자는 위조하기 쉽다.
 */
public record PresignedUploadRequest(
        @NotBlank @Size(max = 255)
        @Schema(description = "원본 파일명. key에는 쓰이지 않는다", example = "receipt.jpg")
        String filename,
        @NotBlank
        @Schema(
                description = "이미지 MIME 타입",
                allowableValues = {"image/png", "image/jpeg"},
                example = "image/jpeg")
        String contentType,
        @NotNull @Positive
        @Schema(description = "이미지 바이트 크기. 5MB를 넘을 수 없다", example = "204800")
        Long size) {}
