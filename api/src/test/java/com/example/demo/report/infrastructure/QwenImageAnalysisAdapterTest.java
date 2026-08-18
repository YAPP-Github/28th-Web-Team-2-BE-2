package com.example.demo.report.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import com.example.demo.external.qwen.QwenChatRequest;
import com.example.demo.external.qwen.QwenChatResponse;
import com.example.demo.external.qwen.feign.QwenVisionClient;
import com.example.demo.image.application.port.ImageReadUrlPort;
import com.example.demo.report.application.contract.ExtractedPriceTag;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import feign.Request;
import feign.RetryableException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class QwenImageAnalysisAdapterTest {

    private static final String IMAGE_URL = "https://cdn.example.com/images/abc.jpg";
    private static final String READ_URL = "https://s3.example.com/images/abc.jpg?X-Amz-Signature=x";

    private QwenVisionClient qwenVisionClient;
    private ImageReadUrlPort imageReadUrlPort;
    private QwenImageAnalysisAdapter adapter;

    @BeforeEach
    void setUp() {
        qwenVisionClient = mock(QwenVisionClient.class);
        imageReadUrlPort = mock(ImageReadUrlPort.class);
        when(imageReadUrlPort.presignedReadUrl(IMAGE_URL)).thenReturn(READ_URL);
        adapter = new QwenImageAnalysisAdapter(
                qwenVisionClient,
                imageReadUrlPort,
                new PriceTagResponseParser(new ObjectMapper()),
                "qwen-vl-plus");
    }

    @Test
    void 인식_결과를_내부_타입으로_돌려준다() {
        givenContent("""
                {"itemName":"오이","itemConfidence":0.96,"price":250,"numberCount":1}""");

        final ExtractedPriceTag result = adapter.analyze(IMAGE_URL);

        assertThat(result.itemName()).isEqualTo("오이");
        assertThat(result.price()).isEqualTo(250);
    }

    // 버킷을 공개하지 않으므로 영구 URL로는 모델이 이미지를 가져갈 수 없다.
    @Test
    void 영구_URL이_아니라_만료되는_읽기_URL을_모델에_넘긴다() {
        givenContent("{\"numberCount\":0}");

        adapter.analyze(IMAGE_URL);

        verify(imageReadUrlPort).presignedReadUrl(IMAGE_URL);
        final ArgumentCaptor<QwenChatRequest> captor = ArgumentCaptor.forClass(QwenChatRequest.class);
        verify(qwenVisionClient).complete(captor.capture());
        assertThat(captor.getValue().messages().getLast().content().getFirst().imageUrl().url())
                .isEqualTo(READ_URL);
    }

    @Test
    void 설정한_모델을_사용한다() {
        givenContent("{\"numberCount\":0}");

        adapter.analyze(IMAGE_URL);

        final ArgumentCaptor<QwenChatRequest> captor = ArgumentCaptor.forClass(QwenChatRequest.class);
        verify(qwenVisionClient).complete(captor.capture());
        assertThat(captor.getValue().model()).isEqualTo("qwen-vl-plus");
    }

    @Test
    void 본문이_없는_응답은_빈_결과로_다룬다() {
        when(qwenVisionClient.complete(any())).thenReturn(new QwenChatResponse(List.of()));

        final ExtractedPriceTag result = adapter.analyze(IMAGE_URL);

        assertThat(result.itemName()).isNull();
        assertThat(result.price()).isNull();
    }

    // timeout·rate limit·모델 오류가 같은 응답으로 보이면 클라이언트가 재시도 여부를 판단할 수 없다.
    @Test
    void timeout은_504로_구분한다() {
        when(qwenVisionClient.complete(any())).thenThrow(new RetryableException(
                -1, "read timed out", Request.HttpMethod.POST,
                new SocketTimeoutException("read timed out"), (Long) null, request()));

        assertThatThrownBy(() -> adapter.analyze(IMAGE_URL))
                .isInstanceOf(ApiException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.IMAGE_ANALYSIS_TIMEOUT);
    }

    @Test
    void rate_limit은_429로_구분한다() {
        when(qwenVisionClient.complete(any())).thenThrow(new RetryableException(
                429, "too many requests", Request.HttpMethod.POST, (Long) null, request()));

        assertThatThrownBy(() -> adapter.analyze(IMAGE_URL))
                .isInstanceOf(ApiException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.IMAGE_ANALYSIS_RATE_LIMITED);
    }

    @Test
    void 재시도_소진된_서버_오류는_502로_끝낸다() {
        when(qwenVisionClient.complete(any())).thenThrow(new RetryableException(
                503, "unavailable", Request.HttpMethod.POST, (Long) null, request()));

        assertThatThrownBy(() -> adapter.analyze(IMAGE_URL))
                .isInstanceOf(ApiException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.IMAGE_ANALYSIS_UNAVAILABLE);
    }

    @Test
    void 재시도하지_않는_4xx도_502로_끝낸다() {
        when(qwenVisionClient.complete(any()))
                .thenThrow(new FeignException.BadRequest("bad request", request(), null, null));

        assertThatThrownBy(() -> adapter.analyze(IMAGE_URL))
                .isInstanceOf(ApiException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.IMAGE_ANALYSIS_UNAVAILABLE);
    }

    @Test
    void JSON이_아닌_본문은_인식_실패로_끝낸다() {
        givenContent("사진을 읽을 수 없습니다");

        assertThatThrownBy(() -> adapter.analyze(IMAGE_URL))
                .isInstanceOf(ApiException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.IMAGE_ANALYSIS_UNAVAILABLE);
    }

    private void givenContent(final String content) {
        when(qwenVisionClient.complete(any())).thenReturn(new QwenChatResponse(
                List.of(new QwenChatResponse.Choice(
                        new QwenChatResponse.Message("assistant", content), "stop"))));
    }

    private Request request() {
        return Request.create(
                Request.HttpMethod.POST,
                "https://dashscope-intl.aliyuncs.com/compatible-mode/v1/chat/completions",
                Collections.emptyMap(),
                new byte[0],
                StandardCharsets.UTF_8,
                null);
    }
}
