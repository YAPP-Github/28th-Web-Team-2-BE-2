package com.example.demo.image.domain;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import org.springframework.http.HttpStatus;

/**
 * 업로드를 허용하는 이미지 형식.
 *
 * <p>계약({@code .agents/skills/image-upload-flow/SKILL.md})이 PNG와 JPEG만 허용하고 key 확장자를
 * {@code png} 또는 {@code jpg}로 못 박아 두었다. 확장자와 MIME을 한 자리에 묶어 두면 둘이 어긋날
 * 수 없다.
 */
public enum ImageContentType {
    PNG("image/png", "png"),
    JPEG("image/jpeg", "jpg");

    private final String mimeType;
    private final String extension;

    ImageContentType(final String mimeType, final String extension) {
        this.mimeType = mimeType;
        this.extension = extension;
    }

    /**
     * MIME 문자열을 형식으로 바꾼다. 허용 목록에 없으면 400으로 끝낸다.
     *
     * <p>{@code null}이나 빈 값은 형식을 판별할 근거가 없어 거부한다.
     */
    public static ImageContentType from(final String mimeType) {
        if (mimeType == null || mimeType.isBlank()) {
            throw invalidFormat();
        }
        return switch (mimeType.trim().toLowerCase()) {
            case "image/png" -> PNG;
            // image/jpg는 표준이 아니지만 일부 브라우저와 이전 클라이언트가 보낸다.
            case "image/jpeg", "image/jpg" -> JPEG;
            default -> throw invalidFormat();
        };
    }

    private static ApiException invalidFormat() {
        return new ApiException(
                ErrorType.INVALID_IMAGE_FORMAT.description(),
                ErrorType.INVALID_IMAGE_FORMAT,
                HttpStatus.BAD_REQUEST);
    }

    public String mimeType() {
        return mimeType;
    }

    public String extension() {
        return extension;
    }
}
