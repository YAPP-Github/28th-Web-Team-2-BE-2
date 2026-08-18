package com.example.demo.image.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ImageDomainTest {

    @Test
    void 허용한_MIME을_형식으로_바꾼다() {
        assertThat(ImageContentType.from("image/png")).isEqualTo(ImageContentType.PNG);
        assertThat(ImageContentType.from("image/jpeg")).isEqualTo(ImageContentType.JPEG);
    }

    @Test
    void 대소문자와_공백이_섞인_MIME도_받아들인다() {
        assertThat(ImageContentType.from("  IMAGE/PNG  ")).isEqualTo(ImageContentType.PNG);
    }

    // 표준 MIME은 image/jpeg지만 일부 클라이언트가 image/jpg를 보낸다.
    @Test
    void 비표준_image_jpg를_JPEG로_받아들인다() {
        assertThat(ImageContentType.from("image/jpg")).isEqualTo(ImageContentType.JPEG);
    }

    @ParameterizedTest
    @ValueSource(strings = {"image/gif", "image/webp", "application/pdf", "text/plain", "image"})
    void 허용하지_않는_MIME은_거부한다(final String mimeType) {
        assertThatThrownBy(() -> ImageContentType.from(mimeType))
                .isInstanceOf(ApiException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.INVALID_IMAGE_FORMAT);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void MIME이_없으면_형식을_판별하지_않고_거부한다(final String mimeType) {
        assertThatThrownBy(() -> ImageContentType.from(mimeType))
                .isInstanceOf(ApiException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.INVALID_IMAGE_FORMAT);
    }

    @Test
    void 확장자는_MIME과_함께_고정된다() {
        assertThat(ImageContentType.PNG.extension()).isEqualTo("png");
        assertThat(ImageContentType.JPEG.extension()).isEqualTo("jpg");
    }

    @Test
    void key는_images_접두사와_형식_확장자를_가진다() {
        final ImageKey key = ImageKey.generate(ImageContentType.JPEG);

        assertThat(key.value()).matches("^images/[0-9a-f-]{36}\\.jpg$");
    }

    @Test
    void key는_매번_다른_UUID로_생성된다() {
        assertThat(ImageKey.generate(ImageContentType.PNG).value())
                .isNotEqualTo(ImageKey.generate(ImageContentType.PNG).value());
    }

    @ParameterizedTest
    @ValueSource(strings = {"receipt.jpg", "../images/evil.png", "uploads/a.png"})
    void 접두사가_없는_key는_만들_수_없다(final String value) {
        assertThatThrownBy(() -> new ImageKey(value)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 상한_이하_크기는_허용한다() {
        assertThat(new ImageSize(5L * 1024 * 1024).bytes()).isEqualTo(5L * 1024 * 1024);
    }

    @Test
    void 상한을_넘는_크기는_IMAGE_TOO_LARGE로_거부한다() {
        assertThatThrownBy(() -> new ImageSize(5L * 1024 * 1024 + 1))
                .isInstanceOf(ApiException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.IMAGE_TOO_LARGE);
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, -1L})
    void 빈_이미지는_형식_오류로_거부한다(final long bytes) {
        assertThatThrownBy(() -> new ImageSize(bytes))
                .isInstanceOf(ApiException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.INVALID_IMAGE_FORMAT);
    }
}
