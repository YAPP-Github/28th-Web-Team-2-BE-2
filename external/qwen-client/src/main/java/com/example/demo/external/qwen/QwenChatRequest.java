package com.example.demo.external.qwen;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Qwen vision 호출 본문. DashScope의 OpenAI 호환 모드({@code /compatible-mode/v1/chat/completions})
 * 스키마다.
 *
 * <p>와이어 필드명은 {@code @JsonProperty}로 명시한다. 이 레포의 ObjectMapper 는 기본
 * LOWER_CAMEL_CASE 라서({@code common.config.api.JacksonConfig}) 애노테이션 없이는
 * {@code response_format}이 {@code responseFormat}으로 나가 무시된다.
 *
 * <p>{@code responseFormat} 지원 여부는 공식 문서로 확인하지 못했다 — 지원하지 않는 계정·모델이면
 * 무시되거나 400 이 날 수 있다. 호출자가 프롬프트에도 JSON 지시를 넣고 스키마를 검증할 것을
 * 전제한다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record QwenChatRequest(
        String model,
        List<QwenMessage> messages,
        Double temperature,
        @JsonProperty("max_tokens") Integer maxTokens,
        @JsonProperty("response_format") ResponseFormat responseFormat) {

    /** 추출 작업이라 표본 다양성이 필요 없다. 같은 사진에 같은 답이 나오는 편이 낫다. */
    private static final Double DETERMINISTIC = 0.0d;

    private static final ResponseFormat JSON_OBJECT = new ResponseFormat("json_object");

    /**
     * 상한을 두지 않으면 출력이 잘렸을 때 깨진 JSON 문자열이 오고, 그게 파싱 실패로만 드러난다.
     * 추출 스키마는 짧으므로 넉넉히 잡아도 이 값에 닿지 않는다.
     */
    private static final Integer MAX_TOKENS = 1024;

    public static QwenChatRequest jsonOnly(final String model, final List<QwenMessage> messages) {
        return new QwenChatRequest(model, messages, DETERMINISTIC, MAX_TOKENS, JSON_OBJECT);
    }

    /** {@code response_format}. JSON 객체만 받겠다는 선언이다. */
    public record ResponseFormat(String type) {}
}
