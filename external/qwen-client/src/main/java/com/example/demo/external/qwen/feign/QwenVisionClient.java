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
 * <p>base URL은 설정으로 둔다. DashScope는 리전에 따라 host가 갈리고(중국 본토·국제),
 * workspace 전용 endpoint 형태도 문서에 함께 나온다. 코드에 박으면 계정 설정이 바뀔 때마다
 * 배포가 필요하다.
 */
@FeignClient(
        name = "qwenVisionClient",
        url = "${qwen.vision.url}",
        configuration = QwenClientConfiguration.class)
public interface QwenVisionClient {

    @PostMapping(path = "/chat/completions", consumes = MediaType.APPLICATION_JSON_VALUE)
    QwenChatResponse complete(@RequestBody QwenChatRequest request);
}
