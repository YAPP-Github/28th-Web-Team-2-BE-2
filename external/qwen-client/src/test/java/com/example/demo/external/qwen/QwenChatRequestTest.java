package com.example.demo.external.qwen;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import java.util.List;
import org.junit.jupiter.api.Test;

class QwenChatRequestTest {

    // Feign이 사용하는 Spring Boot ObjectMapper와 같은 명명 전략을 재현한다.
    private final ObjectMapper objectMapper = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

    @Test
    void JSON만_받겠다고_선언한다() {
        final QwenChatRequest request = QwenChatRequest.jsonOnly(
                "qwen-vl-plus", List.of(QwenMessage.system("추출 규칙")));

        assertThat(request.responseFormat().type()).isEqualTo("json_object");
    }

    // 같은 사진에 매번 다른 값이 나오면 사용자가 결과를 신뢰할 수 없다.
    @Test
    void 추출_작업이라_temperature를_0으로_고정한다() {
        final QwenChatRequest request = QwenChatRequest.jsonOnly("qwen-vl-plus", List.of());

        assertThat(request.temperature()).isZero();
    }

    @Test
    void 이미지_파트는_image_url_타입으로_직렬화된다() throws Exception {
        final QwenMessage message = QwenMessage.userWithImage("가격을 읽어라", "https://cdn/x.jpg");

        final String json = objectMapper.writeValueAsString(message);

        assertThat(json).contains("\"type\":\"image_url\"");
        assertThat(json).contains("\"url\":\"https://cdn/x.jpg\"");
    }

    // 지시가 이미지를 가리키는 순서라야 모델이 맥락을 잡는다.
    @Test
    void 사용자_메시지는_이미지를_먼저_담는다() {
        final QwenMessage message = QwenMessage.userWithImage("가격을 읽어라", "https://cdn/x.jpg");

        assertThat(message.content().getFirst().type()).isEqualTo("image_url");
        assertThat(message.content().getLast().type()).isEqualTo("text");
    }

    @Test
    void 비어_있는_필드는_직렬화에서_제외한다() throws Exception {
        final String json = objectMapper.writeValueAsString(QwenContent.text("설명"));

        assertThat(json).doesNotContain("image_url");
    }

    @Test
    void system_메시지는_텍스트_파트만_가진다() {
        final QwenMessage message = QwenMessage.system("규칙");

        assertThat(message.role()).isEqualTo("system");
        assertThat(message.content()).singleElement().extracting(QwenContent::text).isEqualTo("규칙");
    }
}
