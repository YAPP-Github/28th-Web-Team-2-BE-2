package com.example.demo.image.domain;

import com.example.demo.common.exception.ErrorType;
import com.example.demo.common.exception.ImageValidationException;
import java.util.Locale;

/**
 * 업로드 파일의 Content-Type과 key 확장자.
 *
 * <p>모바일 카메라가 보내는 HEIC·HEIF·WEBP 등 형식이 계속 늘어나므로 MIME 허용목록이나
 * 고정된 이미지 시그니처를 두지 않는다. 빈 본문만 업로드 경계에서 거부하고, 실제 MIME과
 * 확장자는 전달받은 값을 정규화해 저장한다.
 */
public record ImageContentType(String mimeType, String extension) {

    public static final ImageContentType PNG = new ImageContentType("image/png", "png");
    public static final ImageContentType JPEG = new ImageContentType("image/jpeg", "jpg");

    private static final String DEFAULT_MIME_TYPE = "application/octet-stream";

    public ImageContentType {
        if (mimeType == null || mimeType.isBlank() || extension == null || extension.isBlank()) {
            throw invalidFormat();
        }
    }

    /**
     * 모바일 클라이언트가 보낸 MIME을 정규화한다. MIME이 없거나 알 수 없는 경우에도 업로드를
     * 막지 않도록 generic binary 타입으로 저장한다.
     */
    public static ImageContentType from(final String mimeType, final byte[] content) {
        if (content == null || content.length == 0) {
            throw invalidFormat();
        }
        final String normalizedMimeType = normalizeMimeType(mimeType);
        return new ImageContentType(normalizedMimeType, extensionFromMimeType(normalizedMimeType));
    }

    private static ImageValidationException invalidFormat() {
        return new ImageValidationException(ErrorType.INVALID_IMAGE_FORMAT);
    }

    private static String normalizeMimeType(final String mimeType) {
        if (mimeType == null || mimeType.isBlank()) {
            return DEFAULT_MIME_TYPE;
        }
        final String normalized = mimeType.trim().toLowerCase(Locale.ROOT);
        final int parametersStart = normalized.indexOf(';');
        final String withoutParameters = parametersStart < 0
                ? normalized
                : normalized.substring(0, parametersStart).trim();
        return "image/jpg".equals(withoutParameters) ? "image/jpeg" : withoutParameters;
    }

    private static String extensionFromMimeType(final String mimeType) {
        final int separator = mimeType.lastIndexOf('/');
        if (separator < 0 || separator == mimeType.length() - 1) {
            return "bin";
        }
        if ("image/jpeg".equals(mimeType)) {
            return "jpg";
        }
        if ("image/png".equals(mimeType)) {
            return "png";
        }
        final String extension = mimeType.substring(separator + 1).replaceAll("[^a-z0-9]", "");
        return extension.isBlank() ? "bin" : extension;
    }
}
