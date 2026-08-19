package com.example.demo.image.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * presigned PUT 발급 요청. 계약이 정한 세 필드다.
 *
 * <p>{@code filename}은 서버가 읽지 않는다. key 는 {@code ImageKey}가 UUID 로 만들고 형식은
 * {@code contentType}으로만 판별한다 — 확장자는 위조하기 쉽다. 계약에 있는 필드라 받아만 두므로
 * 필수로 두지 않는다(서버가 무시하는 값이 비었다고 400 을 낼 이유가 없다).
 */
public record PresignedUploadRequest(
        @Size(max = 255)
        @Schema(
                description = "원본 파일명. 서버는 읽지 않는다 — key 는 UUID 로 생성하고 형식은 "
                        + "contentType 으로만 판별한다. 계약에 있는 필드라 받아만 둔다",
                example = "receipt.jpg",
                nullable = true)
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
