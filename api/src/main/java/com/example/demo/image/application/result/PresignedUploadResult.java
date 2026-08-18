package com.example.demo.image.application.result;

import java.time.Instant;

/**
 * presigned PUT 발급 결과.
 *
 * <p>{@code uploadUrl}은 만료되는 값이고 {@code imageUrl}은 영구 값이다. 계약이 "만료되는 upload
 * URL을 게시글이나 사용자 데이터에 저장하지 않는다"고 정한 이유가 이 구분이다. 제보에 저장할 값은
 * 항상 {@code imageUrl}이다.
 */
public record PresignedUploadResult(
        String uploadUrl,
        String imageUrl,
        String method,
        Instant expiresAt,
        String contentType) {

    /** 계약이 method를 항상 PUT으로 고정한다. */
    public static final String PUT_METHOD = "PUT";
}
