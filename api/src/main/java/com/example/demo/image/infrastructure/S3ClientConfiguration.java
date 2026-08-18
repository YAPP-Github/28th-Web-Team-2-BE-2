package com.example.demo.image.infrastructure;

import java.net.URI;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import org.springframework.beans.factory.annotation.Value;

/**
 * S3 클라이언트 구성.
 *
 * <p>자격증명은 {@link DefaultCredentialsProvider}로만 얻는다. 운영에서는 EC2 인스턴스 역할
 * (IMDS)이 공급하므로 access key를 설정에 두지 않는다.
 *
 * <p>{@code aws.s3.endpoint}는 로컬·테스트에서 S3 호환 저장소를 가리키기 위한 선택 설정이다.
 * 비어 있으면 실제 AWS endpoint를 쓴다. 경로 스타일 접근도 그때만 켠다 — 가상 호스트 스타일은
 * 버킷 이름이 DNS에 있어야 해서 로컬 저장소에서 동작하지 않는다.
 */
@Configuration
public class S3ClientConfiguration {

    private final String region;
    private final String endpoint;

    public S3ClientConfiguration(
            @Value("${aws.region:ap-northeast-2}") final String region,
            @Value("${aws.s3.endpoint:}") final String endpoint) {
        this.region = region;
        this.endpoint = endpoint;
    }

    @Bean
    @ConditionalOnMissingBean(S3Client.class)
    S3Client s3Client() {
        final S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create());
        if (hasCustomEndpoint()) {
            return builder.endpointOverride(URI.create(endpoint)).forcePathStyle(true).build();
        }
        return builder.build();
    }

    @Bean
    @ConditionalOnMissingBean(S3Presigner.class)
    S3Presigner s3Presigner() {
        final S3Presigner.Builder builder = S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create());
        if (hasCustomEndpoint()) {
            return builder.endpointOverride(URI.create(endpoint)).build();
        }
        return builder.build();
    }

    private boolean hasCustomEndpoint() {
        return endpoint != null && !endpoint.isBlank();
    }
}
