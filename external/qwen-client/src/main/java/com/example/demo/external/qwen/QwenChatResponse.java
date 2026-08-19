package com.example.demo.external.qwen;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Optional;

/**
 * Qwen 응답. 우리가 쓰는 건 첫 choice의 메시지 본문 하나다.
 *
 * <p>모델이 반환하는 본문은 "JSON 문자열"이다. 즉 이 응답을 파싱한 뒤 그 안의 문자열을 한 번 더
 * 파싱해야 한다. 그 두 번째 파싱과 스키마 검증은 이 모듈이 아니라 호출하는 쪽 책임으로 둔다 —
 * 추출 스키마는 서비스 도메인 지식이고, 외부 클라이언트 모듈이 알 이유가 없다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record QwenChatResponse(List<Choice> choices) {

    /** 본문이 비어 있는 응답(모델이 아무것도 답하지 않은 경우)도 있으므로 Optional로 좁힌다. */
    public Optional<String> firstContent() {
        if (choices == null || choices.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(choices.getFirst())
                .map(Choice::message)
                .map(Message::content)
                .filter(content -> !content.isBlank());
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Choice(Message message, @JsonProperty("finish_reason") String finishReason) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Message(String role, String content) {}
}
