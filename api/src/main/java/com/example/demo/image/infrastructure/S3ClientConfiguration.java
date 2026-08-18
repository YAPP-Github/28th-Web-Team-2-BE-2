package com.example.demo.image.infrastructure;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * S3 클라이언트 구성.
 *
 * <p>자격증명은 {@link DefaultCredentialsProvider}로만 얻는다. 운영에서는 EC2 인스턴스 역할
 * (IMDS)이 공급하므로 access key를 설정에 두지 않는다.
 */
@Configuration
public class S3ClientConfiguration {

    private final String region;

    public S3ClientConfiguration(@Value("${aws.region:ap-northeast-2}") final String region) {
        this.region = region;
    }

    @Bean
    S3Client s3Client() {
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    @Bean
    S3Presigner s3Presigner() {
        return S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
}
