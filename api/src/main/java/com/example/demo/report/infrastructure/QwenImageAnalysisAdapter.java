package com.example.demo.report.infrastructure;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import com.example.demo.external.qwen.QwenChatRequest;
import com.example.demo.external.qwen.QwenChatResponse;
import com.example.demo.external.qwen.QwenMessage;
import com.example.demo.external.qwen.feign.QwenVisionClient;
import com.example.demo.image.application.port.ImageUrlPort;
import com.example.demo.report.application.contract.ExtractedPriceTag;
import com.example.demo.report.application.port.ImageAnalysisPort;
import feign.FeignException;
import feign.RetryableException;
import java.io.InterruptedIOException;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Qwen vision 어댑터.
 *
 * <p>실패 의미 판정을 이 클래스 한 곳에 모은다. {@code GlobalExceptionHandler}가
 * {@code FeignException}을 일괄 502 {@code EXTERNAL_API_ERROR}로 바꾸기 때문에, 여기서
 * {@link ApiException}으로 바꾸지 않으면 timeout·rate limit·모델 오류가 사용자에게 모두 같은
 * 응답으로 보인다.
 *
 * <p>모델에는 공개 읽기 영구 URL을 그대로 넘긴다. 다만 그 URL이 우리 저장소의 것인지는 확인한다.
 */
@Component
public class QwenImageAnalysisAdapter implements ImageAnalysisPort {

    private final QwenVisionClient qwenVisionClient;
    private final ImageUrlPort imageUrlPort;
    private final PriceTagResponseParser parser;
    private final String model;

    public QwenImageAnalysisAdapter(
            final QwenVisionClient qwenVisionClient,
            final ImageUrlPort imageUrlPort,
            final PriceTagResponseParser parser,
            @Value("${qwen.vision.model}") final String model) {
        this.qwenVisionClient = qwenVisionClient;
        this.imageUrlPort = imageUrlPort;
        this.parser = parser;
        this.model = model;
    }

    @Override
    public ExtractedPriceTag analyze(final String imageUrl) {
        // images/ 접두사는 공개 읽기라 서명이 필요 없다. 우리 URL 인지만 확인한다 — 임의 URL 을
        // 넘기면 사용자가 우리 비용으로 아무 호스트나 가져오게 만들 수 있다.
        final QwenChatResponse response = call(imageUrlPort.requireOwnedUrl(imageUrl));
        return response.firstContent().map(parser::parse).orElseThrow(this::emptyResponse);
    }

    private QwenChatResponse call(final String readUrl) {
        try {
            return qwenVisionClient.complete(request(readUrl));
        } catch (final RetryableException exception) {
            throw classifyRetryable(exception);
        } catch (final FeignException exception) {
            throw unavailable(exception);
        }
    }

    private QwenChatRequest request(final String readUrl) {
        return QwenChatRequest.jsonOnly(
                model,
                List.of(
                        QwenMessage.system(PriceTagPrompt.SYSTEM),
                        QwenMessage.userWithImage(PriceTagPrompt.USER, readUrl)));
    }

    /**
     * 재시도를 모두 소진한 경우다. 원인을 나눠 사용자가 다시 시도할지 판단할 수 있게 한다.
     *
     * <p>timeout은 Feign이 {@code IOException}을 감싸 여기까지 온다. 429는 디코더가 상태 코드를
     * 남겨 두었다.
     */
    private ApiException classifyRetryable(final RetryableException exception) {
        if (isTimeout(exception)) {
            return new ApiException(
                    ErrorType.IMAGE_ANALYSIS_TIMEOUT.description(),
                    ErrorType.IMAGE_ANALYSIS_TIMEOUT,
                    HttpStatus.GATEWAY_TIMEOUT,
                    exception);
        }
        if (isRateLimited(exception)) {
            // 429 가 아니라 503 이다. 한도를 넘긴 건 클라이언트가 아니라 우리 모델 쿼터다.
            // 429 를 주면 클라이언트가 자기 호출을 줄여야 한다고 오해한다.
            return new ApiException(
                    ErrorType.IMAGE_ANALYSIS_RATE_LIMITED.description(),
                    ErrorType.IMAGE_ANALYSIS_RATE_LIMITED,
                    HttpStatus.SERVICE_UNAVAILABLE,
                    exception);
        }
        return unavailable(exception);
    }

    private boolean isTimeout(final Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            // SocketTimeoutException 도 InterruptedIOException 이다.
            if (current instanceof InterruptedIOException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private boolean isRateLimited(final RetryableException exception) {
        return exception.status() == HttpStatus.TOO_MANY_REQUESTS.value();
    }

    /**
     * 모델이 {@code choices} 를 비워 보낸 경우.
     *
     * <p>파서가 비-JSON 본문에 쓰는 것과 같은 코드로 끝낸다 — 같은 신호(프롬프트·모델 설정 오류)인데
     * 한쪽만 조용히 빈 결과를 주면 "인식했지만 아무것도 못 읽음"으로 보여 원인이 묻힌다.
     */
    private ApiException emptyResponse() {
        return new ApiException(
                ErrorType.IMAGE_ANALYSIS_INVALID_RESPONSE.description(),
                ErrorType.IMAGE_ANALYSIS_INVALID_RESPONSE,
                HttpStatus.BAD_GATEWAY);
    }

    private ApiException unavailable(final Throwable cause) {
        return new ApiException(
                ErrorType.IMAGE_ANALYSIS_UNAVAILABLE.description(),
                ErrorType.IMAGE_ANALYSIS_UNAVAILABLE,
                HttpStatus.BAD_GATEWAY,
                cause);
    }
}
