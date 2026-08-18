package com.example.demo.external.qwen;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import java.util.List;
import org.junit.jupiter.api.Test;

class QwenChatResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

    @Test
    void 첫_choice의_본문을_꺼낸다() {
        final QwenChatResponse response = new QwenChatResponse(
                List.of(new QwenChatResponse.Choice(
                        new QwenChatResponse.Message("assistant", "{\"item\":\"오이\"}"), "stop")));

        assertThat(response.firstContent()).contains("{\"item\":\"오이\"}");
    }

    @Test
    void choice가_없으면_비어_있다() {
        assertThat(new QwenChatResponse(List.of()).firstContent()).isEmpty();
        assertThat(new QwenChatResponse(null).firstContent()).isEmpty();
    }

    // 모델이 아무 말도 하지 않는 응답도 온다. 빈 문자열을 파싱 단계로 흘리면 원인이 흐려진다.
    @Test
    void 본문이_비어_있으면_비어_있는_것으로_다룬다() {
        final QwenChatResponse response = new QwenChatResponse(
                List.of(new QwenChatResponse.Choice(
                        new QwenChatResponse.Message("assistant", "   "), "stop")));

        assertThat(response.firstContent()).isEmpty();
    }

    @Test
    void 메시지가_없는_choice도_비어_있는_것으로_다룬다() {
        final QwenChatResponse response = new QwenChatResponse(
                List.of(new QwenChatResponse.Choice(null, "stop")));

        assertThat(response.firstContent()).isEmpty();
    }

    // 모델·provider가 필드를 추가해도 역직렬화가 깨지면 안 된다.
    @Test
    void 모르는_필드가_있어도_역직렬화한다() throws Exception {
        final String json = """
                {"id":"chat-1","model":"qwen-vl-plus","usage":{"total_tokens":10},
                 "choices":[{"index":0,"finish_reason":"stop",
                             "message":{"role":"assistant","content":"{}","extra":1}}]}""";

        final QwenChatResponse response = objectMapper.readValue(json, QwenChatResponse.class);

        assertThat(response.firstContent()).contains("{}");
        assertThat(response.choices().getFirst().finishReason()).isEqualTo("stop");
    }
}
