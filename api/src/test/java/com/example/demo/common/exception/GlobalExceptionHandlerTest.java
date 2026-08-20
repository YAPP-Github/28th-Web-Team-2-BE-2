package com.example.demo.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import feign.FeignException;
import feign.Request;
import feign.Response;
import java.util.Map;
import org.junit.jupiter.api.Test;
import com.example.demo.common.presentation.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

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

    // 처리하지 않으면 서블릿 상한 초과 업로드가 400 IMAGE_TOO_LARGE 대신 500으로 나간다.
    @Test
    void 서블릿_상한을_넘긴_업로드는_IMAGE_TOO_LARGE_400으로_변환한다() {
        final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/images");

        final ResponseEntity<?> result = handler.handleMaxUploadSizeExceeded(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(result.getBody()).isEqualTo(new ApiResponse<>(
                ErrorType.IMAGE_TOO_LARGE.name(), ErrorType.IMAGE_TOO_LARGE.description(), null));
    }

    @Test
    void v1이_아닌_경로의_상한_초과는_legacy_오류_형식을_유지한다() {
        final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/images");

        final ResponseEntity<?> result = handler.handleMaxUploadSizeExceeded(request);

        assertThat(result.getBody()).isEqualTo(new ApiErrorResponse(
                ErrorType.IMAGE_TOO_LARGE.name(), ErrorType.IMAGE_TOO_LARGE.description()));
    }
}
