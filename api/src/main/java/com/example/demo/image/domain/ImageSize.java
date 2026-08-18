package com.example.demo.image.domain;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import org.springframework.http.HttpStatus;

/**
 * 업로드 이미지의 바이트 크기. 계약이 5MB 상한을 정하고 있다.
 *
 * <p>상한 검증을 이 타입에 두는 이유는 검증 지점이 둘이기 때문이다. 서버 경유 multipart는
 * 실제 바이트 수를, presigned 발급은 클라이언트가 신고한 {@code size}를 검증한다. 규칙이 한 곳에
 * 있으면 두 경로가 갈라지지 않는다.
 *
 * <p>presigned 경로의 신고값은 신뢰할 수 없다는 점에 유의한다. 실제 강제는 S3가 서명에 묶인
 * {@code Content-Length}로 수행하고, 여기서는 잘못된 요청을 미리 끊는 역할만 한다.
 */
public record ImageSize(long bytes) {

    private static final long MAX_BYTES = 5L * 1024 * 1024;

    public ImageSize {
        if (bytes <= 0) {
            throw new ApiException(
                    ErrorType.INVALID_IMAGE_FORMAT.description(),
                    ErrorType.INVALID_IMAGE_FORMAT,
                    HttpStatus.BAD_REQUEST);
        }
        if (bytes > MAX_BYTES) {
            throw new ApiException(
                    ErrorType.IMAGE_TOO_LARGE.description(),
                    ErrorType.IMAGE_TOO_LARGE,
                    HttpStatus.BAD_REQUEST);
        }
    }
}
