package com.example.demo.external.qwen.feign;

import com.example.demo.external.qwen.QwenChatRequest;
import com.example.demo.external.qwen.QwenChatResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Qwen vision 호출.
 *
 * <p>base URL 과 model 은 설정으로 둔다. 미검증 사항 정리는 {@code docs/adr/0002-qwen-vision-client.md}.
 */
@FeignClient(
        name = "qwenVisionClient",
        url = "${qwen.vision.url}",
        configuration = QwenVisionClientConfiguration.class)
public interface QwenVisionClient {

    @PostMapping(path = "/chat/completions", consumes = MediaType.APPLICATION_JSON_VALUE)
    QwenChatResponse complete(@RequestBody QwenChatRequest request);
}
