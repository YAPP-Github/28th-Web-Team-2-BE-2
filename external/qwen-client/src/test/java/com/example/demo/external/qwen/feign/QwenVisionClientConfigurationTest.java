package com.example.demo.external.qwen.feign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import feign.RequestTemplate;
import feign.Request;
import feign.RetryableException;
import feign.Retryer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class QwenVisionClientConfigurationTest {

    private final QwenVisionClientConfiguration configuration = new QwenVisionClientConfiguration();

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void API_key가_비어있으면_설정에_실패한다(final String apiKey) {
        assertThatThrownBy(() -> configuration.qwenRequestInterceptor(apiKey))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.errorType()).isEqualTo(ErrorType.CONFIGURATION_ERROR));
    }

    // 예외 메시지에 키가 새면 로그와 에러 응답을 통해 유출된다.
    @Test
    void 설정_실패_메시지에_키_값을_담지_않는다() {
        assertThatThrownBy(() -> configuration.qwenRequestInterceptor(""))
                .hasMessage(ErrorType.CONFIGURATION_ERROR.description());
    }

    @Test
    void API_key를_Bearer_헤더로_붙인다() {
        final RequestTemplate template = new RequestTemplate();

        configuration.qwenRequestInterceptor("secret-key").apply(template);

        assertThat(template.headers().get("Authorization")).containsExactly("Bearer secret-key");
    }

    // 이전 이름은 "설정한 횟수를 따른다"였지만 isInstanceOf 만 봐서 횟수를 검증하지 않았다.
    @Test
    void 재시도를_소진하면_예외를_전파한다() {
        final Retryer retryer = configuration.qwenRetryer(1L, 2);
        final Request request = Request.create(
                Request.HttpMethod.POST, "https://qwen.test/chat/completions",
                java.util.Map.of(), new byte[0], java.nio.charset.StandardCharsets.UTF_8, null);
        final RetryableException failure = new RetryableException(
                503, "unavailable", Request.HttpMethod.POST, (Long) null, request);

        retryer.continueOrPropagate(failure);

        assertThatThrownBy(() -> retryer.continueOrPropagate(failure)).isSameAs(failure);
    }

    /**
     * yaml 의 키 이름과 {@code @Value} 의 키 이름이 어긋나면 잡는다.
     *
     * <p>이 회귀가 한 번 있었다 — 코드는 {@code qwen.vision-retry.*} 를 읽는데 yaml 은
     * {@code qwen.retry.*} 였고, {@code @Value} 에 기본값이 있어서 조용히 동작했다. Feign 빈은
     * lazy 라 컨텍스트 테스트도 생성하지 않아 CI 가 통과했다. 기본값을 없앤 뒤로는 키가 없으면
     * 여기서 터진다.
     */
    @Test
    void 재시도_설정_키가_yaml_에_있다() {
        assertThat(readQwenBlock())
                .as("코드가 읽는 키가 yaml 에 있어야 한다")
                .contains("vision-retry:", "period-ms:", "max-attempts:");
    }

    private String readQwenBlock() {
        try {
            final var path = java.nio.file.Path.of("..", "..", "api", "src", "main", "resources",
                    "application.yaml");
            return java.nio.file.Files.readString(path);
        } catch (final java.io.IOException exception) {
            throw new IllegalStateException("application.yaml 을 읽을 수 없다", exception);
        }
    }

    @Test
    void 오류_디코더를_Qwen_전용으로_등록한다() {
        assertThat(configuration.qwenErrorDecoder()).isInstanceOf(QwenErrorDecoder.class);
    }
}
