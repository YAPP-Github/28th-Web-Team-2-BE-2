package com.example.demo.external.qwen;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

class QwenChatRequestTest {

    // common.config.api.JacksonConfig 의 프로덕션 빈과 동일하게 맨 ObjectMapper 를 쓴다.
    // SNAKE_CASE 를 재현하던 이전 버전은 실제 와이어와 달라 image_url 누락을 통과시켰다.
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 이전 테스트는 우리가 방금 넣은 값을 되읽기만 해서 와이어 키를 검증하지 못했다.
    @Test
    void 요청_본문은_response_format_키로_직렬화된다() throws Exception {
        final QwenChatRequest request = QwenChatRequest.jsonOnly(
                "qwen-vl-plus", List.of(QwenMessage.system("추출 규칙")));

        final String json = objectMapper.writeValueAsString(request);

        assertThat(json).contains("\"response_format\":{\"type\":\"json_object\"}");
        assertThat(json).doesNotContain("responseFormat");
    }

    // 같은 사진에 매번 다른 값이 나오면 사용자가 결과를 신뢰할 수 없다.
    @Test
    void 추출_작업이라_temperature를_0으로_고정한다() {
        final QwenChatRequest request = QwenChatRequest.jsonOnly("qwen-vl-plus", List.of());

        assertThat(request.temperature()).isZero();
    }

    @Test
    void system_메시지는_텍스트_파트만_가진다() {
        final QwenMessage message = QwenMessage.system("규칙");

        assertThat(message.role()).isEqualTo("system");
        assertThat(message.content()).singleElement().extracting(QwenContent::text).isEqualTo("규칙");
    }
}
