package com.example.demo.image.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.demo.common.exception.ErrorType;
import com.example.demo.common.exception.ImageValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
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
    @ValueSource(strings = {"image/gif", "image/webp", "image/heic", "image/heif", "application/octet-stream"})
    void 모바일_이미지_MIME은_허용한다(final String mimeType) {
        assertThat(ImageContentType.from(mimeType, PNG_BYTES).mimeType()).isEqualTo(mimeType);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void MIME이_없어도_기본_Content_Type으로_허용한다(final String mimeType) {
        assertThat(ImageContentType.from(mimeType, PNG_BYTES).mimeType())
                .isEqualTo("application/octet-stream");
    }

    @Test
    void 확장자는_MIME과_함께_고정된다() {
        assertThat(ImageContentType.PNG.extension()).isEqualTo("png");
        assertThat(ImageContentType.JPEG.extension()).isEqualTo("jpg");
    }

    @Test
    void MIME만_정규화하고_파일_시그니처는_제한하지_않는다() {
        assertThat(ImageContentType.from("image/png", PNG_BYTES)).isEqualTo(ImageContentType.PNG);
        assertThat(ImageContentType.from("image/jpeg", JPEG_BYTES)).isEqualTo(ImageContentType.JPEG);
    }

    @Test
    void 모바일_이미지의_알려지지_않은_시그니처도_허용한다() {
        final byte[] zip = {0x50, 0x4B, 0x03, 0x04, 1, 2, 3, 4, 5};

        assertThat(ImageContentType.from("image/heic", zip).mimeType()).isEqualTo("image/heic");
    }

    @Test
    void 빈_내용만_형식_오류로_거부한다() {
        assertThat(ImageContentType.from("image/heic", new byte[] {(byte) 0x01}).mimeType())
                .isEqualTo("image/heic");
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

    @Test
    void 확장자_허용목록없이_대문자만_소문자로_정규화한다() {
        assertThat(ImageKey.generate(ImageContentType.JPEG, "WEBP").value())
                .matches("^images/[0-9a-f-]{36}\\.webp$");
    }

    @Test
    void 가게_이미지_key는_같은_가게에서_같다() {
        assertThat(ImageKey.forStore(42L, ImageContentType.PNG))
                .isEqualTo(ImageKey.forStore(42L, ImageContentType.PNG));
        assertThat(ImageKey.forStore(42L, ImageContentType.PNG))
                .isNotEqualTo(ImageKey.forStore(43L, ImageContentType.PNG));
    }

    @ParameterizedTest
    @ValueSource(strings = {"receipt.jpg", "../images/evil.png", "uploads/a.png", "images/"})
    void 형식을_벗어난_key는_만들_수_없다(final String value) {
        assertThatThrownBy(() -> new ImageKey(value)).isInstanceOf(ImageValidationException.class);
    }

    // 접두사만 검사하면 images/../secret 같은 값이 통과한다.
    @ParameterizedTest
    @ValueSource(strings = {"images/../secret.png", "images/a/b.png"})
    void 상위_경로로_빠져나가는_key를_거부한다(final String value) {
        assertThatThrownBy(() -> new ImageKey(value)).isInstanceOf(ImageValidationException.class);
    }

    // 외부 입력 경로는 500이 아니라 400이어야 한다.
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"uploads/a.png", "images/../secret.png", ""})
    void 잘못된_key는_400으로_끝낸다(final String value) {
        assertThatThrownBy(() -> new ImageKey(value))
                .isInstanceOf(ImageValidationException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.INVALID_PARAMETER_ERROR);
    }

    @Test
    void 생성한_key는_다시_해석할_수_있다() {
        final ImageKey generated = ImageKey.generate(ImageContentType.PNG);

        assertThat(new ImageKey(generated.value())).isEqualTo(generated);
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
