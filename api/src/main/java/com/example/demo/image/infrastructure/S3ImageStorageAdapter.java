package com.example.demo.image.infrastructure;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import com.example.demo.image.application.command.UploadImageCommand;
import com.example.demo.image.application.port.ImageStoragePort;
import com.example.demo.image.application.port.ImageUrlPort;
import com.example.demo.image.domain.ImageKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * S3 구현. SDK 예외를 밖으로 흘리지 않고 계약이 정한 503으로 바꾼다.
 *
 * <p>503 응답에는 bucket 이름이나 SDK 메시지를 담지 않는다. 계약이 "내부 정보 없이" 반환하라고
 * 정했고, 저장소 구성은 공격자에게 알려 줄 이유가 없는 정보다. 원인은 예외 cause로만 남긴다.
 */
@Slf4j
@Component
public class S3ImageStorageAdapter implements ImageStoragePort, ImageUrlPort {

    private final S3Client s3Client;
    private final S3ImageStorageProperties properties;

    public S3ImageStorageAdapter(
            final S3Client s3Client, final S3ImageStorageProperties properties) {
        this.s3Client = s3Client;
        this.properties = properties;
    }

    @Override
    public String uploadAndReturnUrl(final ImageKey key, final UploadImageCommand command) {
        log.info(
                "image storage upload started key={} contentType={} sizeBytes={}",
                key.value(), command.contentType().mimeType(), command.size().bytes());
        // URL 을 PUT 전에 확정한다. baseUrl 설정이 비어 있으면 여기서 503 이 나고, 객체를 올린 뒤
        // 503 을 내보내 참조 불가능한 고아 객체가 쌓이는 일이 없다.
        final String imageUrl = permanentUrl(key);
        // contentLength 는 넘기지 않는다. 동기 putObject 는 본문을 aws-chunked 로 감싸며 이 값을
        // 덮고 실제 크기는 RequestBody 에서 가져간다 — 와이어에 나가지 않는 죽은 설정이다.
        final PutObjectRequest request = PutObjectRequest.builder()
                .bucket(properties.requireBucket())
                .key(key.value())
                .contentType(command.contentType().mimeType())
                .build();
        try {
            s3Client.putObject(request, RequestBody.fromBytes(command.content()));
        } catch (final SdkException exception) {
            log.error(
                    "image storage upload failed key={} contentType={} sizeBytes={}",
                    key.value(), command.contentType().mimeType(), command.size().bytes(), exception);
            throw storageUnavailable(exception);
        }
        log.info(
                "image storage upload completed key={} contentType={} sizeBytes={}",
                key.value(), command.contentType().mimeType(), command.size().bytes());
        return imageUrl;
    }

    /**
     * 우리 저장소 URL 인지만 본다.
     *
     * <p>영구 URL 은 {@code baseUrl + key} 규칙으로만 만들어지므로 접두사를 떼면 key 가 된다.
     * {@link ImageKey} 생성자가 {@code images/{name}.{extension}} 형식을 강제하므로 상위 경로 이탈과
     * 쿼리스트링 부착도 함께 걸러진다.
     */
    @Override
    public String requireOwnedUrl(final String imageUrl) {
        final String baseUrl = properties.requireBaseUrl();
        if (imageUrl == null || !imageUrl.startsWith(baseUrl)) {
            throw ApiException.invalidParameter();
        }
        // 생성자가 형식을 강제한다. 어긋나면 400 이다.
        new ImageKey(imageUrl.substring(baseUrl.length()));
        return imageUrl;
    }

    private String permanentUrl(final ImageKey key) {
        return properties.requireBaseUrl() + key.value();
    }

    private ApiException storageUnavailable(final Throwable cause) {
        // 응답에는 내부 정보를 담지 않는다(계약). 원인은 예외 cause로만 남긴다.
        return new ApiException(
                ErrorType.IMAGE_STORAGE_UNAVAILABLE.description(),
                ErrorType.IMAGE_STORAGE_UNAVAILABLE,
                HttpStatus.SERVICE_UNAVAILABLE,
                cause);
    }
}
