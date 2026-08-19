package com.example.demo.image.domain;

import com.example.demo.common.exception.ErrorType;
import com.example.demo.common.exception.ImageValidationException;
import java.util.Arrays;
import java.util.Locale;

/**
 * 업로드를 허용하는 이미지 형식.
 *
 * <p>계약({@code .agents/skills/image-upload-flow/SKILL.md})이 PNG와 JPEG만 허용하고 key 확장자를
 * {@code png} 또는 {@code jpg}로 못 박아 두었다. MIME과 확장자를 enum 상수 한 자리에만 적어 두면
 * 형식을 추가할 때 고칠 곳이 하나다.
 */
public enum ImageContentType {
    PNG("image/png", "png"),
    JPEG("image/jpeg", "jpg");

    /** 표준이 아니지만 일부 브라우저와 이전 클라이언트가 보낸다. */
    private static final String JPEG_ALIAS = "image/jpg";

    private static final byte[] PNG_SIGNATURE = {
        (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };

    private static final byte[] JPEG_SIGNATURE = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};

    private final String mimeType;
    private final String extension;

    ImageContentType(final String mimeType, final String extension) {
        this.mimeType = mimeType;
        this.extension = extension;
    }

    /**
     * MIME 문자열을 형식으로 바꾼다. 허용 목록에 없으면 거부한다.
     *
     * <p>{@code null}이나 빈 값은 형식을 판별할 근거가 없어 거부한다.
     */
    public static ImageContentType from(final String mimeType) {
        if (mimeType == null || mimeType.isBlank()) {
            throw invalidFormat();
        }
        // Locale.ROOT 를 명시한다 — tr_TR 로케일에서 "IMAGE/PNG" 가 "ımage/png" 가 된다.
        final String normalized = mimeType.trim().toLowerCase(Locale.ROOT);
        for (final ImageContentType candidate : values()) {
            if (candidate.mimeType.equals(normalized)) {
                return candidate;
            }
        }
        if (JPEG_ALIAS.equals(normalized)) {
            return JPEG;
        }
        throw invalidFormat();
    }

    /**
     * 선언된 형식과 실제 바이트가 맞는지 본다.
     *
     * <p>신고된 {@code Content-Type}만 믿으면 인증 사용자가 우리 도메인을 임의 파일 호스트로 쓸 수
     * 있다. 서버를 거치는 경로는 바이트를 손에 들고 있으므로 선두 시그니처를 대조한다.
     */
    public boolean matchesSignature(final byte[] content) {
        final byte[] signature = signature();
        if (content == null || content.length < signature.length) {
            return false;
        }
        return Arrays.equals(content, 0, signature.length, signature, 0, signature.length);
    }

    private byte[] signature() {
        return switch (this) {
            case PNG -> PNG_SIGNATURE;
            case JPEG -> JPEG_SIGNATURE;
        };
    }

    private static ImageValidationException invalidFormat() {
        return new ImageValidationException(ErrorType.INVALID_IMAGE_FORMAT);
    }

    public String mimeType() {
        return mimeType;
    }

    public String extension() {
        return extension;
    }
}
