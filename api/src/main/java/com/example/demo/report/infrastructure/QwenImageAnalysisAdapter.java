package com.example.demo.report.infrastructure;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import com.example.demo.external.qwen.QwenChatRequest;
import com.example.demo.external.qwen.QwenChatResponse;
import com.example.demo.external.qwen.QwenMessage;
import com.example.demo.external.qwen.feign.QwenVisionClient;
import com.example.demo.image.application.port.ImageReadUrlPort;
import com.example.demo.report.application.contract.ExtractedPriceTag;
import com.example.demo.report.application.port.ImageAnalysisPort;
import feign.FeignException;
import feign.RetryableException;
import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;
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
 * <p>외부 모델에 넘기는 것은 영구 URL이 아니라 짧은 만료 읽기 URL이다. 버킷을 공개하지 않고도
 * 모델이 이미지를 가져갈 수 있고, 유출되더라도 유효 기간이 짧다.
 */
@Component
public class QwenImageAnalysisAdapter implements ImageAnalysisPort {

    private final QwenVisionClient qwenVisionClient;
    private final ImageReadUrlPort imageReadUrlPort;
    private final PriceTagResponseParser parser;
    private final String model;

    public QwenImageAnalysisAdapter(
            final QwenVisionClient qwenVisionClient,
            final ImageReadUrlPort imageReadUrlPort,
            final PriceTagResponseParser parser,
            @Value("${qwen.vision.model}") final String model) {
        this.qwenVisionClient = qwenVisionClient;
        this.imageReadUrlPort = imageReadUrlPort;
        this.parser = parser;
        this.model = model;
    }

    @Override
    public ExtractedPriceTag analyze(final String imageUrl) {
        final String readUrl = imageReadUrlPort.presignedReadUrl(imageUrl);
        final QwenChatResponse response = call(readUrl);
        return response.firstContent().map(parser::parse).orElseGet(ExtractedPriceTag::empty);
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
            return new ApiException(
                    ErrorType.IMAGE_ANALYSIS_RATE_LIMITED.description(),
                    ErrorType.IMAGE_ANALYSIS_RATE_LIMITED,
                    HttpStatus.TOO_MANY_REQUESTS,
                    exception);
        }
        return unavailable(exception);
    }

    private boolean isTimeout(final Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof SocketTimeoutException || current instanceof InterruptedIOException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private boolean isRateLimited(final RetryableException exception) {
        return exception.status() == HttpStatus.TOO_MANY_REQUESTS.value();
    }

    private ApiException unavailable(final Throwable cause) {
        return new ApiException(
                ErrorType.IMAGE_ANALYSIS_UNAVAILABLE.description(),
                ErrorType.IMAGE_ANALYSIS_UNAVAILABLE,
                HttpStatus.BAD_GATEWAY,
                cause);
    }
}
