package com.example.demo.image.domain;

import com.example.demo.common.exception.ErrorType;
import com.example.demo.common.exception.ImageValidationException;
import java.util.Arrays;
import java.util.Locale;

/**
 * 업로드를 허용하는 이미지 형식.
 *
 * <p>PNG와 JPEG의 실제 형식과 MIME을 한 곳에서 검증한다. 저장 key의 확장자는 업로드 파일명에서
 * 별도로 정규화하므로 이 타입의 canonical 확장자와 반드시 같을 필요는 없다.
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
     * 신고된 MIME 과 실제 바이트가 함께 맞을 때만 형식을 돌려준다.
     *
     * <p>둘을 한 팩터리로 묶은 이유가 있다. 신고된 {@code Content-Type}만 믿으면 인증 사용자가
     * 우리 버킷을 임의 파일 호스트로 쓸 수 있고, 버킷이 공개 읽기라 그 파일이 우리 도메인에서
     * 서비스된다. 호출부가 바이트 대조를 잊을 수 있는 형태로 두지 않는다.
     *
     * <p>{@code null}이나 빈 MIME 은 형식을 판별할 근거가 없어 거부한다.
     */
    public static ImageContentType from(final String mimeType, final byte[] content) {
        final ImageContentType resolved = resolve(mimeType);
        if (!resolved.matchesSignature(content)) {
            throw invalidFormat();
        }
        return resolved;
    }

    private static ImageContentType resolve(final String mimeType) {
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

    private boolean matchesSignature(final byte[] content) {
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
