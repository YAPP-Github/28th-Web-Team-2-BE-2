package com.example.demo.external.kamis.feign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Request;
import feign.Response;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;

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

        assertThat(exception).isInstanceOf(KamisClientException.class);
        final KamisClientException kamisException = (KamisClientException) exception;
        assertThat(kamisException.getMessage()).isEqualTo("인증 정보가 올바르지 않습니다.");
        assertThat(kamisException.status()).isEqualTo(401);
    }
}
