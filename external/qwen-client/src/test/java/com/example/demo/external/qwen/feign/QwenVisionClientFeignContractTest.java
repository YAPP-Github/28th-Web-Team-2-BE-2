package com.example.demo.external.qwen.feign;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.external.qwen.QwenChatResponse;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.openfeign.FeignClient;

/** kakao·kamis 와 같은 계약 테스트. 이 모듈에만 없었다. */
class QwenVisionClientFeignContractTest {

    @Test
    void Qwen_vision_클라이언트의_Feign_설정을_선언한다() {
        final FeignClient annotation = QwenVisionClient.class.getAnnotation(FeignClient.class);
        final Map<String, Class<?>> returnTypes = Arrays.stream(QwenVisionClient.class.getDeclaredMethods())
                .collect(Collectors.toMap(Method::getName, Method::getReturnType));

        assertThat(annotation).isNotNull();
        assertThat(annotation.name()).isEqualTo("qwenVisionClient");
        assertThat(annotation.url()).isEqualTo("${qwen.vision.url}");
        assertThat(annotation.configuration()).containsExactly(QwenClientConfiguration.class);
        assertThat(returnTypes).containsEntry("complete", QwenChatResponse.class);
    }
}
