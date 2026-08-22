package com.example.demo.image.domain;

import com.example.demo.common.exception.ErrorType;
import com.example.demo.common.exception.ImageValidationException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * S3 객체 key. {@code images/{UUID}.{extension}} 형식을 강제한다.
 *
 * <p>key를 문자열로 들고 다니면 어디서든 조립할 수 있게 되어 형식이 갈라진다. 생성자가 형식을
 * 강제하므로 어긋난 값은 만들어지지 않는다.
 *
 * <p>클라이언트 파일명 전체는 key에 넣지 않고 확장자만 사용한다. 경로 조작({@code ../})과
 * 한글·공백 같은 서명 깨지는 문자를 key에 섞지 않는다.
 */
public record ImageKey(String value) {

    private static final String PREFIX = "images/";

    /**
     * 허용 형식. 접두사 뒤에 슬래시를 허용하지 않으므로 {@code images/../secret}처럼 상위 경로로
     * 빠져나가는 값이 걸러진다.
     */
    private static final Pattern FORMAT = Pattern.compile("^images/[^/]+\\.[A-Za-z0-9]+$");
    private static final Pattern EXTENSION = Pattern.compile("^[a-z0-9]+$");

    /**
     * 형식이 어긋나면 400 으로 끝낸다({@link ImageValidationException}).
     *
     * <p>{@link #generate} 는 형식을 지키므로 실패할 수 없고, 실패하는 경로는 외부 입력뿐이다.
     * 그래서 예외 종류를 하나만 둔다 — {@code IllegalArgumentException} 을 흘리면
     * {@code GlobalExceptionHandler} 에 catch-all 이 없어 클라이언트가 400 대신 500 을 받는다.
     */
    public ImageKey {
        if (value == null || !FORMAT.matcher(value).matches()) {
            throw new ImageValidationException(ErrorType.INVALID_PARAMETER_ERROR);
        }
    }

    public static ImageKey generate(final ImageContentType contentType) {
        if (contentType == null) {
            throw new ImageValidationException(ErrorType.INVALID_PARAMETER_ERROR);
        }
        return generate(contentType, contentType.extension());
    }

    public static ImageKey generate(final ImageContentType contentType, final String extension) {
        if (contentType == null) {
            throw new ImageValidationException(ErrorType.INVALID_PARAMETER_ERROR);
        }
        final String normalizedExtension = extension == null
                ? contentType.extension()
                : extension.trim().toLowerCase(Locale.ROOT);
        if (!EXTENSION.matcher(normalizedExtension).matches()) {
            throw new ImageValidationException(ErrorType.INVALID_PARAMETER_ERROR);
        }
        return new ImageKey(PREFIX + UUID.randomUUID() + "." + normalizedExtension);
    }

    public static ImageKey forStore(final Long storeId, final ImageContentType contentType) {
        if (storeId == null || storeId <= 0 || contentType == null) {
            throw new ImageValidationException(ErrorType.INVALID_PARAMETER_ERROR);
        }
        final UUID stableId = UUID.nameUUIDFromBytes(
                ("store:" + storeId).getBytes(StandardCharsets.UTF_8));
        return new ImageKey(PREFIX + stableId + "." + contentType.extension());
    }

}
