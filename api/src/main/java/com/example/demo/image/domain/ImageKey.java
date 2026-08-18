package com.example.demo.image.domain;

import com.example.demo.common.exception.ApiException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * S3 객체 key. 계약이 {@code images/{UUID}.{png|jpg}}로 고정한 형식이다.
 *
 * <p>key를 문자열로 들고 다니면 어디서든 조립할 수 있게 되어 형식이 갈라진다. 생성 경로를
 * {@link #generate(ImageContentType)}와 {@link #of(String)} 둘로 좁혀 두면 형식이 어긋날 수 없다.
 *
 * <p>클라이언트가 보낸 파일명은 key에 넣지 않는다. 경로 조작({@code ../})과 한글·공백 같은
 * 서명 깨지는 문자를 매번 걸러 내는 대신, 이름을 아예 쓰지 않는 편이 안전하다.
 */
public record ImageKey(String value) {

    private static final String PREFIX = "images/";

    /**
     * 허용 형식. 접두사 뒤에 슬래시를 허용하지 않으므로 {@code images/../secret}처럼 상위 경로로
     * 빠져나가는 값이 걸러진다.
     */
    private static final Pattern FORMAT = Pattern.compile("^images/[^/]+\\.(png|jpg)$");

    public ImageKey {
        if (value == null || !FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("image key must match " + FORMAT.pattern());
        }
    }

    public static ImageKey generate(final ImageContentType contentType) {
        return new ImageKey(PREFIX + UUID.randomUUID() + "." + contentType.extension());
    }

    /**
     * 외부에서 들어온 값을 key로 해석한다.
     *
     * <p>형식이 어긋나면 400으로 끝낸다. 생성자의 {@link IllegalArgumentException}을 그대로
     * 흘리면 {@code GlobalExceptionHandler}에 catch-all이 없어 클라이언트가 500과 공통 envelope이
     * 아닌 body를 받는다. 잘못된 요청은 잘못된 요청으로 알려야 한다.
     */
    public static ImageKey of(final String value) {
        if (value == null || !FORMAT.matcher(value).matches()) {
            throw ApiException.invalidParameter();
        }
        return new ImageKey(value);
    }
}
