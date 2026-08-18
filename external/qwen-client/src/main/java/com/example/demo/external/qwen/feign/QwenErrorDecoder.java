package com.example.demo.external.qwen.feign;

import feign.Request;
import feign.Response;
import feign.RetryableException;
import feign.codec.ErrorDecoder;
import org.springframework.http.HttpStatus;

/**
 * 재시도할 수 있는 실패만 골라 {@link RetryableException}으로 감싼다.
 *
 * <p>이 디코더는 상태 코드를 의미로 바꾸지 않는다. 그 판단은 {@code QwenImageAnalysisAdapter}가
 * 한 곳에서 한다. 여기서 {@code ApiException}으로 바꿔 버리면 재시도 소진 후 Feign이 다시 던지는
 * 예외에 그 분류가 남지 않아(RetryableException이 cause를 그대로 전달하지 않는 경로가 있다)
 * 결국 정보가 사라진다.
 *
 * <p>재시도 대상은 429와 5xx다. 이미지 인식은 조회성 호출이라 같은 요청을 다시 보내도 안전하다.
 * 나머지 4xx는 우리 요청 자체가 잘못된 경우이므로 다시 보내도 같은 답이 온다 — 기본 디코더에
 * 넘겨 상태 코드를 가진 {@code FeignException}으로 만든다.
 *
 * <p>연결·읽기 timeout은 여기까지 오지 않는다. Feign이 {@code IOException}을 자체적으로
 * {@link RetryableException}으로 감싸 {@code Retryer}에 넘긴다.
 */
public final class QwenErrorDecoder implements ErrorDecoder {

    private static final int TOO_MANY_REQUESTS = HttpStatus.TOO_MANY_REQUESTS.value();

    private final ErrorDecoder delegate = new ErrorDecoder.Default();

    @Override
    public Exception decode(final String methodKey, final Response response) {
        final Exception decoded = delegate.decode(methodKey, response);
        if (isRetryable(response.status())) {
            return asRetryable(decoded, response);
        }
        return decoded;
    }

    private boolean isRetryable(final int status) {
        return status == TOO_MANY_REQUESTS || status >= HttpStatus.INTERNAL_SERVER_ERROR.value();
    }

    private RetryableException asRetryable(final Exception decoded, final Response response) {
        return new RetryableException(
                response.status(),
                decoded.getMessage(),
                requestMethod(response),
                decoded,
                (Long) null,
                response.request());
    }

    private Request.HttpMethod requestMethod(final Response response) {
        if (response.request() == null) {
            return Request.HttpMethod.POST;
        }
        return response.request().httpMethod();
    }
}
