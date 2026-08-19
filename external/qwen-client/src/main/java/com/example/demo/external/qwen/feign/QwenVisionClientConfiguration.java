package com.example.demo.external.qwen.feign;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import feign.RequestInterceptor;
import feign.Retryer;
import feign.codec.ErrorDecoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;

/**
 * Qwen vision 클라이언트 구성. 이름은 형제 규약({@code <클라이언트>Configuration})을 따른다.
 * API key 검증은 Kakao 클라이언트와 같은 방식이다.
 *
 * <p>키를 로그·에러 응답에 남기지 않는다. 누락 시 예외 메시지에도 키 값이나 설정 경로를 담지 않고
 * {@code CONFIGURATION_ERROR}만 알린다.
 */
public class QwenVisionClientConfiguration {

    @Bean
    public RequestInterceptor qwenRequestInterceptor(@Value("${qwen.api-key}") final String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ApiException(
                    ErrorType.CONFIGURATION_ERROR.description(),
                    ErrorType.CONFIGURATION_ERROR,
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return requestTemplate -> requestTemplate.header("Authorization", "Bearer " + apiKey);
    }

    @Bean
    public ErrorDecoder qwenErrorDecoder() {
        return new QwenErrorDecoder();
    }

    /**
     * 재시도 정책.
     *
     * <p>인식은 사용자가 화면에서 기다리는 동기 요청이다. 재시도를 많이 하면 응답이 늦어지는 쪽이
     * 더 나쁘므로 총 2회 시도(최초 1회 + 재시도 1회)로 제한한다. 대상은 429·5xx와 timeout이다.
     *
     * <p>간격을 {@code Duration}이 아니라 밀리초로 받는다. Feign은 클라이언트마다 별도 자식
     * 컨텍스트를 만드는데 그 컨텍스트에는 {@code ApplicationConversionService}가 없어
     * {@code "500ms"} 같은 문자열을 {@code Duration}으로 바꾸지 못한다.
     *
     * <p>maxPeriod 노브는 두지 않는다. 간격이 {@code period * 1.5^attempt}이고 시도가 2회면
     * 한 번(750ms)만 잠들므로 상한에 닿을 수 없다.
     */
    @Bean
    public Retryer qwenRetryer(
            @Value("${qwen.vision-retry.period-ms:500}") final long periodMs,
            @Value("${qwen.vision-retry.max-attempts:2}") final int maxAttempts) {
        return new Retryer.Default(periodMs, periodMs * maxAttempts, maxAttempts);
    }
}
