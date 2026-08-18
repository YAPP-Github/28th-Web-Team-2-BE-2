package com.example.demo.external.qwen;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 메시지 파트. {@code text} 또는 {@code image_url} 하나만 채운다.
 *
 * <p>이미지는 URL로 전달한다. 공식 문서에 나오는 형태이고, 5MB base64(약 6.7MB 본문)를 매 호출마다
 * 실어 보내는 것보다 가볍다. 우리 쪽에서는 짧은 만료 presigned GET URL을 넘겨 버킷을 공개하지 않는다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record QwenContent(String type, String text, QwenImageUrl imageUrl) {

    private static final String TYPE_TEXT = "text";
    private static final String TYPE_IMAGE_URL = "image_url";

    public static QwenContent text(final String text) {
        return new QwenContent(TYPE_TEXT, text, null);
    }

    public static QwenContent imageUrl(final String url) {
        return new QwenContent(TYPE_IMAGE_URL, null, new QwenImageUrl(url));
    }

    public record QwenImageUrl(String url) {}
}
