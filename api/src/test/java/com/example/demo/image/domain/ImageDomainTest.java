package com.example.demo.image.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.demo.common.exception.ImageValidationException;
import com.example.demo.common.exception.ErrorType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ImageDomainTest {

    private static final byte[] PNG_BYTES = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 1};
    private static final byte[] JPEG_BYTES = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 1};

    @Test
    void 허용한_MIME을_형식으로_바꾼다() {
        assertThat(ImageContentType.from("image/png", PNG_BYTES)).isEqualTo(ImageContentType.PNG);
        assertThat(ImageContentType.from("image/jpeg", JPEG_BYTES)).isEqualTo(ImageContentType.JPEG);
    }

    @Test
    void 대소문자와_공백이_섞인_MIME도_받아들인다() {
        assertThat(ImageContentType.from("  IMAGE/PNG  ", PNG_BYTES)).isEqualTo(ImageContentType.PNG);
    }

    // 표준 MIME은 image/jpeg지만 일부 클라이언트가 image/jpg를 보낸다.
    @Test
    void 비표준_image_jpg를_JPEG로_받아들인다() {
        assertThat(ImageContentType.from("image/jpg", JPEG_BYTES)).isEqualTo(ImageContentType.JPEG);
    }

    @ParameterizedTest
    @ValueSource(strings = {"image/gif", "image/webp", "application/pdf", "text/plain", "image"})
    void 허용하지_않는_MIME은_거부한다(final String mimeType) {
        assertThatThrownBy(() -> ImageContentType.from(mimeType, PNG_BYTES))
                .isInstanceOf(ImageValidationException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.INVALID_IMAGE_FORMAT);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void MIME이_없으면_형식을_판별하지_않고_거부한다(final String mimeType) {
        assertThatThrownBy(() -> ImageContentType.from(mimeType, PNG_BYTES))
                .isInstanceOf(ImageValidationException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.INVALID_IMAGE_FORMAT);
    }

    @Test
    void 확장자는_MIME과_함께_고정된다() {
        assertThat(ImageContentType.PNG.extension()).isEqualTo("png");
        assertThat(ImageContentType.JPEG.extension()).isEqualTo("jpg");
    }

    // 신고된 Content-Type 만 믿으면 인증 사용자가 공개 버킷을 임의 파일 호스트로 쓸 수 있다.
    @Test
    void MIME_과_선두_바이트가_함께_맞을_때만_형식을_돌려준다() {
        assertThat(ImageContentType.from("image/png", PNG_BYTES)).isEqualTo(ImageContentType.PNG);
        assertThat(ImageContentType.from("image/jpeg", JPEG_BYTES)).isEqualTo(ImageContentType.JPEG);
    }

    @Test
    void 형식을_위조한_바이트는_거부한다() {
        final byte[] zip = {0x50, 0x4B, 0x03, 0x04, 1, 2, 3, 4, 5};

        assertThatThrownBy(() -> ImageContentType.from("image/png", zip))
                .isInstanceOf(ImageValidationException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.INVALID_IMAGE_FORMAT);
    }

    @Test
    void 시그니처보다_짧은_내용은_거부한다() {
        assertThatThrownBy(() -> ImageContentType.from("image/png", new byte[] {(byte) 0x89, 0x50}))
                .isInstanceOf(ImageValidationException.class);
        assertThatThrownBy(() -> ImageContentType.from("image/jpeg", null))
                .isInstanceOf(ImageValidationException.class);
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
                .isInstanceOf(ImageValidationException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.IMAGE_TOO_LARGE);
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, -1L})
    void 빈_이미지는_형식_오류로_거부한다(final long bytes) {
        assertThatThrownBy(() -> new ImageSize(bytes))
                .isInstanceOf(ImageValidationException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.INVALID_IMAGE_FORMAT);
    }
}
