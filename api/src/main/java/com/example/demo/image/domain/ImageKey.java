package com.example.demo.image.domain;

import java.util.UUID;

/**
 * S3 객체 key. 계약이 {@code images/{UUID}.{png|jpg}}로 고정한 형식이다.
 *
 * <p>key를 문자열로 들고 다니면 어디서든 조립할 수 있게 되어 형식이 갈라진다. 생성 경로를
 * {@link #generate(ImageContentType)} 하나로 좁혀 두면 접두사와 확장자가 어긋날 수 없다.
 *
 * <p>클라이언트가 보낸 파일명은 key에 넣지 않는다. 경로 조작({@code ../})과 한글·공백 같은
 * 서명 깨지는 문자를 매번 걸러 내는 대신, 이름을 아예 쓰지 않는 편이 안전하다.
 */
public record ImageKey(String value) {

    private static final String PREFIX = "images/";

    public ImageKey {
        if (value == null || !value.startsWith(PREFIX)) {
            throw new IllegalArgumentException("image key must start with " + PREFIX);
        }
    }

    public static ImageKey generate(final ImageContentType contentType) {
        return new ImageKey(PREFIX + UUID.randomUUID() + "." + contentType.extension());
    }
}
