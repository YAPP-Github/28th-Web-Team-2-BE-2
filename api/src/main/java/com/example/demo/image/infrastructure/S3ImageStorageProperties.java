package com.example.demo.image.infrastructure;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * 이미지 저장소 설정값.
 *
 * <p>누락 검증을 bean 생성 시점이 아니라 접근 시점에 한다. 계약이 "bucket/base URL 누락 또는 S3
 * 실패는 내부 정보 없이 HTTP 503"을 요구하기 때문이다. 생성 시점에 터뜨리면 설정이 빠진 환경에서
 * 애플리케이션이 아예 뜨지 않아 이미지와 무관한 기능까지 함께 멈춘다. Kakao 클라이언트가
 * {@code CONFIGURATION_ERROR}로 즉시 실패하는 것과 의도적으로 다르다 — 그쪽은 키가 없으면 해당
 * 기능이 존재할 수 없고, 이쪽은 나머지 API가 정상 동작해야 한다.
 */
@Component
public class S3ImageStorageProperties {

    private final String bucket;
    private final String baseUrl;
    private final Duration presignExpiry;

    public S3ImageStorageProperties(
            @Value("${aws.s3.bucket:}") final String bucket,
            @Value("${aws.s3.base-url:}") final String baseUrl,
            @Value("${aws.s3.presign-expiry:10m}") final Duration presignExpiry) {
        this.bucket = bucket;
        this.baseUrl = baseUrl;
        this.presignExpiry = presignExpiry;
    }

    public String bucket() {
        return required(bucket);
    }

    /**
     * 영구 URL 접두사. key를 그대로 이어 붙이므로 슬래시로 끝나야 한다.
     *
     * <p>설정값에 슬래시가 빠져도 동작하게 보정한다. 이 값이 틀리면 저장된 URL이 전부 깨지는데,
     * 되돌리려면 DB의 {@code photo_url}을 일괄 수정해야 하므로 관대하게 받는 편이 낫다.
     */
    public String baseUrl() {
        final String value = required(baseUrl);
        if (value.endsWith("/")) {
            return value;
        }
        return value + "/";
    }

    public Duration presignExpiry() {
        return presignExpiry;
    }

    private String required(final String value) {
        if (value == null || value.isBlank()) {
            throw new ApiException(
                    ErrorType.IMAGE_STORAGE_UNAVAILABLE.description(),
                    ErrorType.IMAGE_STORAGE_UNAVAILABLE,
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
        return value;
    }
}
