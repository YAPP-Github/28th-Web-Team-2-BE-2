package com.example.demo.external.qwen;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 메시지 파트. {@code text} 또는 {@code image_url} 하나만 채운다.
 *
 * <p>이미지는 URL로 전달한다. 공식 문서에 나오는 형태이고, 5MB base64(약 6.7MB 본문)를 매 호출마다
 * 실어 보내는 것보다 가볍다. 어떤 URL을 넘길지는 호출자 책임이다.
 *
 * <p>필드명은 {@code @JsonProperty}로 못 박는다. 이 레포의 ObjectMapper 는 기본
 * LOWER_CAMEL_CASE 라서({@code common.config.api.JacksonConfig}) 애노테이션이 없으면
 * {@code imageUrl}로 직렬화되고 모델이 이미지를 받지 못한다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record QwenContent(
        String type, String text, @JsonProperty("image_url") QwenImageUrl imageUrl) {

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
