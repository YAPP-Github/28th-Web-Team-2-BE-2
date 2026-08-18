package com.example.demo.external.qwen;

import java.util.List;

/**
 * 대화 메시지. vision 호출은 {@code content}가 문자열이 아니라 파트 배열이다.
 *
 * <p>{@code system}으로 추출 규칙을, {@code user}로 이미지와 지시를 보낸다.
 */
public record QwenMessage(String role, List<QwenContent> content) {

    private static final String SYSTEM = "system";
    private static final String USER = "user";

    public static QwenMessage system(final String instruction) {
        return new QwenMessage(SYSTEM, List.of(QwenContent.text(instruction)));
    }

    public static QwenMessage userWithImage(final String instruction, final String imageUrl) {
        // 이미지를 먼저 두고 지시를 뒤에 둔다. 지시가 이미지를 가리키는 순서라 모델이 맥락을 잡기 쉽다.
        return new QwenMessage(USER, List.of(QwenContent.imageUrl(imageUrl), QwenContent.text(instruction)));
    }
}
