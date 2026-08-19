package com.example.demo.external.qwen;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

/** QwenMessage·QwenContent 검증. 이전에는 QwenChatRequestTest 안에 있어 파일명으로 찾을 수 없었다. */
class QwenMessageTest {

    // common.config.api.JacksonConfig 의 프로덕션 빈과 동일하게 맨 ObjectMapper 를 쓴다.
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void 이미지_파트는_image_url_타입으로_직렬화된다() throws Exception {
        final QwenMessage message = QwenMessage.userWithImage("가격을 읽어라", "https://cdn/x.jpg");

        final String json = objectMapper.writeValueAsString(message);

        assertThat(json).contains("\"type\":\"image_url\"");
        assertThat(json).contains("\"url\":\"https://cdn/x.jpg\"");
        // 키가 imageUrl 로 나가면 모델이 이미지를 받지 못한다.
        assertThat(json).contains("\"image_url\":{");
        assertThat(json).doesNotContain("imageUrl");
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

}
