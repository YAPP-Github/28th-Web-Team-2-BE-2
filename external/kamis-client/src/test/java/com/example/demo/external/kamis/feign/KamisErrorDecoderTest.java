package com.example.demo.external.kamis.feign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Request;
import feign.Response;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class KamisErrorDecoderTest {

    @Test
    void KAMIS_오류_응답의_메시지와_HTTP_상태를_보존한다() throws IOException {
        final Response.Body body = mock(Response.Body.class);
        when(body.asInputStream()).thenReturn(new ByteArrayInputStream(
                "{\"data\":{\"error_code\":\"1\",\"error_msg\":\"인증 정보가 올바르지 않습니다.\"}}"
                        .getBytes(StandardCharsets.UTF_8)));
        final Response response = Response.builder()
                .status(401)
                .request(Request.create(
                        Request.HttpMethod.GET, "http://kamis.test", Map.of(), (Request.Body) null, null))
                .body(body)
                .build();

        final Exception exception = new KamisErrorDecoder(new ObjectMapper())
                .decode("KamisClient#getDailyPrices", response);

        assertThat(exception).isInstanceOf(ApiException.class);
        final ApiException apiException = (ApiException) exception;
        assertThat(apiException.errorMessage()).isEqualTo("인증 정보가 올바르지 않습니다.");
        assertThat(apiException.errorType()).isEqualTo(ErrorType.EXTERNAL_API_ERROR);
        assertThat(apiException.httpStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void HTTP_오류_본문이_없어도_외부_연동_예외로_변환한다() {
        final Response response = Response.builder()
                .status(503)
                .request(Request.create(
                        Request.HttpMethod.GET, "http://kamis.test", Map.of(), (Request.Body) null, null))
                .build();

        final Exception exception = new KamisErrorDecoder(new ObjectMapper())
                .decode("KamisClient#getDailyPrices", response);

        assertThat(exception).isInstanceOfSatisfying(ApiException.class, apiException -> {
            assertThat(apiException.httpStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
            assertThat(apiException.errorMessage()).isEqualTo("외부 API 호출 중 오류가 발생했습니다.");
        });
    }
}
