package com.example.demo.image.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class S3ImageStoragePropertiesTest {

    private static final Duration TEN_MINUTES = Duration.ofMinutes(10);

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void bucket이_없으면_저장소_사용_불가로_끝낸다(final String bucket) {
        final S3ImageStorageProperties properties =
                new S3ImageStorageProperties(bucket, "https://cdn.example.com/", TEN_MINUTES);

        assertThatThrownBy(properties::bucket)
                .isInstanceOf(ApiException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.IMAGE_STORAGE_UNAVAILABLE);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void base_URL이_없으면_저장소_사용_불가로_끝낸다(final String baseUrl) {
        final S3ImageStorageProperties properties =
                new S3ImageStorageProperties("marketgo-images", baseUrl, TEN_MINUTES);

        assertThatThrownBy(properties::baseUrl)
                .isInstanceOf(ApiException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.IMAGE_STORAGE_UNAVAILABLE);
    }

    // 설정값이 틀리면 저장된 photo_url이 전부 깨지고 되돌리려면 DB를 고쳐야 한다.
    @Test
    void base_URL에_슬래시가_빠져도_key를_이어_붙일_수_있게_보정한다() {
        final S3ImageStorageProperties properties =
                new S3ImageStorageProperties("marketgo-images", "https://cdn.example.com", TEN_MINUTES);

        assertThat(properties.baseUrl()).isEqualTo("https://cdn.example.com/");
    }

    @Test
    void 이미_슬래시로_끝나면_중복해서_붙이지_않는다() {
        final S3ImageStorageProperties properties =
                new S3ImageStorageProperties("marketgo-images", "https://cdn.example.com/", TEN_MINUTES);

        assertThat(properties.baseUrl()).isEqualTo("https://cdn.example.com/");
    }

    @Test
    void 설정이_모두_있으면_그대로_읽는다() {
        final S3ImageStorageProperties properties =
                new S3ImageStorageProperties("marketgo-images", "https://cdn.example.com/", TEN_MINUTES);

        assertThat(properties.bucket()).isEqualTo("marketgo-images");
        assertThat(properties.presignExpiry()).isEqualTo(TEN_MINUTES);
    }
}
