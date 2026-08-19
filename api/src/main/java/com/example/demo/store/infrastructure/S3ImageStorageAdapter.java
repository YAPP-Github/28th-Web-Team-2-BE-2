package com.example.demo.store.infrastructure;

import com.example.demo.store.application.port.ImageStoragePort;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Component
@RequiredArgsConstructor
public class S3ImageStorageAdapter implements ImageStoragePort {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket:}")
    private String bucket;

    @Value("${aws.s3.base-url:}")
    private String baseUrl;

    @Override
    public String upload(final byte[] content, final String contentType, final String extension) {
        if (bucket.isBlank() || baseUrl.isBlank()) {
            throw new IllegalStateException("S3 storage is not configured");
        }
        final String key = "images/" + UUID.randomUUID() + "." + extension;
        s3Client.putObject(
                PutObjectRequest.builder().bucket(bucket).key(key).contentType(contentType).build(),
                RequestBody.fromBytes(content));
        return baseUrl.replaceAll("/$", "") + "/" + key;
    }
}
