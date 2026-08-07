package com.example.demo.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import feign.FeignException;
import feign.Request;
import feign.Response;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void Feign_예외는_BAD_GATEWAY_응답으로_변환한다() {
        final Response response = Response.builder()
                .status(503)
                .request(Request.create(
                        Request.HttpMethod.GET, "http://kamis.test", Map.of(), (Request.Body) null, null))
                .build();

        final ResponseEntity<ApiErrorResponse> result = handler.handleFeignException(
                FeignException.errorStatus("KamisClient#getDailyPrices", response));

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(result.getBody()).isEqualTo(new ApiErrorResponse(
                ErrorType.EXTERNAL_API_ERROR.name(),
                ErrorType.EXTERNAL_API_ERROR.description()));
    }
}
