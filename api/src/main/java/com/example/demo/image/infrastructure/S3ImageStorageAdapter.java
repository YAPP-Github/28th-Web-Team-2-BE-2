package com.example.demo.image.infrastructure;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
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
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

/**
 * S3 구현. SDK 예외를 밖으로 흘리지 않고 계약이 정한 503으로 바꾼다.
 *
 * <p>503 응답에는 bucket 이름이나 SDK 메시지를 담지 않는다. 계약이 "내부 정보 없이" 반환하라고
 * 정했고, 저장소 구성은 공격자에게 알려 줄 이유가 없는 정보다. 원인은 예외 cause로만 남긴다.
 */
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
        final PutObjectRequest request = PutObjectRequest.builder()
                .bucket(properties.bucket())
                .key(key.value())
                .contentType(command.contentType().mimeType())
                .contentLength(command.size().bytes())
                .build();
        try {
            s3Client.putObject(request, RequestBody.fromBytes(command.content()));
        } catch (final SdkException exception) {
            throw storageUnavailable(exception);
        }
        return permanentUrl(key);
    }

    @Override
    public PresignedUploadResult presign(
            final ImageKey key, final ImageContentType contentType, final ImageSize size) {
        // Content-Type과 Content-Length를 서명에 포함한다. 클라이언트가 다른 header로 PUT하면
        // S3가 서명 불일치로 거부하므로, 신고한 크기를 넘겨 올리는 경로가 막힌다.
        final PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(properties.bucket())
                .key(key.value())
                .contentType(contentType.mimeType())
                .contentLength(size.bytes())
                .build();
        final Duration expiry = properties.presignExpiry();
        final PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(expiry)
                .putObjectRequest(objectRequest)
                .build();
        try {
            return toResult(key, contentType, expiry, s3Presigner.presignPutObject(presignRequest).url().toString());
        } catch (final SdkException exception) {
            throw storageUnavailable(exception);
        }
    }

    private PresignedUploadResult toResult(
            final ImageKey key,
            final ImageContentType contentType,
            final Duration expiry,
            final String uploadUrl) {
        return new PresignedUploadResult(
                uploadUrl,
                permanentUrl(key),
                PresignedUploadResult.PUT_METHOD,
                Instant.now().plus(expiry),
                contentType.mimeType());
    }

    private String permanentUrl(final ImageKey key) {
        return properties.baseUrl() + key.value();
    }

    private ApiException storageUnavailable(final Throwable cause) {
        return new ApiException(
                ErrorType.IMAGE_STORAGE_UNAVAILABLE.description(),
                ErrorType.IMAGE_STORAGE_UNAVAILABLE,
                HttpStatus.SERVICE_UNAVAILABLE,
                cause);
    }
}
