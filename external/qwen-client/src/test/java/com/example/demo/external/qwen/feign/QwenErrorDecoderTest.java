package com.example.demo.external.qwen.feign;

import static org.assertj.core.api.Assertions.assertThat;

import feign.Request;
import feign.Response;
import feign.RetryableException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class QwenErrorDecoderTest {

    private final QwenErrorDecoder decoder = new QwenErrorDecoder();

    // 429와 5xx는 잠시 뒤 같은 요청이 성공할 수 있다. 인식은 조회성이라 재시도가 안전하다.
    @ParameterizedTest
    @ValueSource(ints = {429, 500, 502, 503, 504})
    void rate_limit과_서버_오류는_재시도_대상으로_감싼다(final int status) {
        final Exception decoded = decoder.decode("complete", response(status));

        assertThat(decoded).isInstanceOf(RetryableException.class);
    }

    // 우리 요청이 잘못된 경우다. 다시 보내도 같은 답이 오므로 재시도하면 지연만 늘어난다.
    @ParameterizedTest
    @ValueSource(ints = {400, 401, 403, 404, 413, 422})
    void 나머지_4xx는_재시도하지_않는다(final int status) {
        final Exception decoded = decoder.decode("complete", response(status));

        assertThat(decoded).isNotInstanceOf(RetryableException.class);
    }

    // 어댑터가 상태 코드로 timeout·rate limit·기타를 구분하므로 코드가 보존되어야 한다.
    @Test
    void 재시도_예외에_원래_상태_코드를_남긴다() {
        final RetryableException decoded = (RetryableException) decoder.decode("complete", response(429));

        assertThat(decoded.status()).isEqualTo(429);
    }

    @Test
    void 재시도_예외에_원래_예외를_cause로_남긴다() {
        final RetryableException decoded = (RetryableException) decoder.decode("complete", response(503));

        assertThat(decoded).hasCauseInstanceOf(feign.FeignException.class);
    }

    // ErrorDecoder.Default 는 Retry-After 가 있으면 상태코드와 무관하게 재시도 대상으로 만든다.
    @ParameterizedTest
    @ValueSource(ints = {400, 401, 404})
    void Retry_After_헤더가_있어도_4xx는_재시도하지_않는다(final int status) {
        final Exception decoded = decoder.decode("complete", response(status, Map.of(
                "Retry-After", List.of("30"))));

        assertThat(decoded).isNotInstanceOf(RetryableException.class);
    }

    // 서버가 지시한 대기 시각을 버리고 우리 백오프로 덮지 않는지 확인한다.
    @Test
    void rate_limit의_Retry_After를_이중_포장하지_않는다() {
        final Exception decoded = decoder.decode("complete", response(429, Map.of(
                "Retry-After", List.of("30"))));

        assertThat(decoded).isInstanceOf(RetryableException.class);
        assertThat(decoded.getCause()).isNotInstanceOf(RetryableException.class);
    }

    private Response response(final int status) {
        return response(status, Collections.emptyMap());
    }

    private Response response(final int status, final Map<String, java.util.Collection<String>> headers) {
        final Request request = Request.create(
                Request.HttpMethod.POST,
                "https://dashscope-intl.aliyuncs.com/compatible-mode/v1/chat/completions",
                Collections.emptyMap(),
                new byte[0],
                StandardCharsets.UTF_8,
                null);
        return Response.builder()
                .status(status)
                .reason("error")
                .request(request)
                .headers(headers)
                .body(new byte[0])
                .build();
    }
}
