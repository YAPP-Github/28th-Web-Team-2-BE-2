package com.example.demo.external.qwen;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * Qwen vision 호출 본문. DashScope의 OpenAI 호환 모드({@code /compatible-mode/v1/chat/completions})
 * 스키마다.
 *
 * <p>{@code responseFormat}으로 JSON만 응답하도록 요구한다. 다만 이 필드의 지원 여부는 공식 문서에서
 * 확인하지 못했다 — 지원하지 않는 계정·모델이면 무시되거나 400이 날 수 있으므로, 프롬프트에도
 * JSON만 답하라는 지시를 함께 넣고 서버에서 스키마를 검증한다. 어느 쪽이든 파싱은 우리가 책임진다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record QwenChatRequest(
        String model,
        List<QwenMessage> messages,
        Double temperature,
        ResponseFormat responseFormat) {

    /** 추출 작업이라 표본 다양성이 필요 없다. 같은 사진에 같은 답이 나오는 편이 낫다. */
    private static final Double DETERMINISTIC = 0.0d;

    private static final ResponseFormat JSON_OBJECT = new ResponseFormat("json_object");

    public static QwenChatRequest jsonOnly(final String model, final List<QwenMessage> messages) {
        return new QwenChatRequest(model, messages, DETERMINISTIC, JSON_OBJECT);
    }

    /** {@code response_format}. JSON 객체만 받겠다는 선언이다. */
    public record ResponseFormat(String type) {}
}
