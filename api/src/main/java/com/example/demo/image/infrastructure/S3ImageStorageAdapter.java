package com.example.demo.image.infrastructure;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import lombok.extern.slf4j.Slf4j;
import com.example.demo.image.application.command.IssuePresignedUploadCommand;
import com.example.demo.image.application.command.UploadImageCommand;
import com.example.demo.image.application.port.ImageStoragePort;
import com.example.demo.image.application.result.PresignedUploadResult;
import com.example.demo.image.domain.ImageContentType;
import com.example.demo.image.domain.ImageKey;
import com.example.demo.image.domain.ImageSize;
import java.time.Duration;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

/**
 * S3 구현. SDK 예외를 밖으로 흘리지 않고 계약이 정한 503으로 바꾼다.
 *
 * <p>503 응답에는 bucket 이름이나 SDK 메시지를 담지 않는다. 계약이 "내부 정보 없이" 반환하라고
 * 정했고, 저장소 구성은 공격자에게 알려 줄 이유가 없는 정보다. 원인은 예외 cause로만 남긴다.
 */
@Slf4j
@Component
public class S3ImageStorageAdapter implements ImageStoragePort {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final S3ImageStorageProperties properties;

    public S3ImageStorageAdapter(
            final S3Client s3Client,
            final S3Presigner s3Presigner,
            final S3ImageStorageProperties properties) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.properties = properties;
    }

    @Override
    public String upload(final ImageKey key, final UploadImageCommand command) {
        // URL 을 PUT 전에 확정한다. baseUrl 설정이 비어 있으면 여기서 503 이 나고, 객체를 올린 뒤
        // 503 을 내보내 참조 불가능한 고아 객체가 쌓이는 일이 없다.
        final String imageUrl = permanentUrl(key);
        // contentLength 는 넘기지 않는다. 동기 putObject 는 본문을 aws-chunked 로 감싸며 이 값을
        // 덮고 실제 크기는 RequestBody 에서 가져간다 — 와이어에 나가지 않는 죽은 설정이다.
        final PutObjectRequest request = PutObjectRequest.builder()
                .bucket(properties.bucket())
                .key(key.value())
                .contentType(command.contentType().mimeType())
                .build();
        try {
            s3Client.putObject(request, RequestBody.fromBytes(command.content()));
        } catch (final SdkException exception) {
            throw storageUnavailable(exception);
        }
        return imageUrl;
    }

    @Override
    public PresignedUploadResult presign(final ImageKey key, final IssuePresignedUploadCommand command) {
        final ImageContentType contentType = command.contentType();
        // Content-Type과 Content-Length를 서명에 포함한다. 클라이언트가 다른 header로 PUT하면
        // S3가 서명 불일치로 거부하므로, 신고한 크기를 넘겨 올리는 경로가 막힌다.
        final PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(properties.bucket())
                .key(key.value())
                .contentType(contentType.mimeType())
                .contentLength(command.size().bytes())
                .build();
        final Duration expiry = properties.presignExpiry();
        final PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(expiry)
                .putObjectRequest(objectRequest)
                .build();
        try {
            // 만료는 재계산하지 않고 서명이 알려주는 값을 쓴다. 설정을 SigV4 상한(7일)보다 크게
            // 잡으면 재계산 값은 실제 수명보다 뒤를 약속한다.
            final PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(presignRequest);
            return new PresignedUploadResult(
                    presigned.url().toString(),
                    permanentUrl(key),
                    PresignedUploadResult.PUT_METHOD,
                    presigned.expiration(),
                    contentType.mimeType());
        } catch (final SdkException exception) {
            throw storageUnavailable(exception);
        }
    }

    private String permanentUrl(final ImageKey key) {
        return properties.baseUrl() + key.value();
    }

    private ApiException storageUnavailable(final Throwable cause) {
        // 응답에는 내부 정보를 담지 않는다(계약). 대신 로그에 남긴다 — 이게 없으면 "S3 장애"와
        // "환경변수 누락"이 운영자 입장에서 구별되지 않는다.
        log.warn("image storage unavailable", cause);
        return new ApiException(
                ErrorType.IMAGE_STORAGE_UNAVAILABLE.description(),
                ErrorType.IMAGE_STORAGE_UNAVAILABLE,
                HttpStatus.SERVICE_UNAVAILABLE,
                cause);
    }
}
