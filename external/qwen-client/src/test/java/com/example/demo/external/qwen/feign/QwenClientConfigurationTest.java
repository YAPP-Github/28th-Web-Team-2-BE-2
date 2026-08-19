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

class QwenClientConfigurationTest {

    private final QwenClientConfiguration configuration = new QwenClientConfiguration();

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

    @Test
    void 재시도는_설정한_횟수를_따른다() {
        final Retryer retryer = configuration.qwenRetryer(500L, 2000L, 2);

        assertThat(retryer).isInstanceOf(Retryer.Default.class);
    }

    @Test
    void 오류_디코더를_Qwen_전용으로_등록한다() {
        assertThat(configuration.qwenErrorDecoder()).isInstanceOf(QwenErrorDecoder.class);
    }
}
