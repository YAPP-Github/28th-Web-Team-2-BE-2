package com.example.demo.external.kakao.feign;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import feign.Request;
import feign.Response;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;

class KakaoErrorDecoderTest {

    @ParameterizedTest
    @ValueSource(ints = {401, 429, 503})
    void Kakao_HTTP_오류를_BAD_GATEWAY_공통_API_예외로_변환한다(final int status) {
        final Response response = Response.builder()
                .status(status)
                .request(Request.create(
                        Request.HttpMethod.GET,
                        "https://dapi.kakao.com",
                        Map.of(),
                        (Request.Body) null,
                        null))
                .build();

        final Exception exception = new KakaoErrorDecoder()
                .decode("KakaoMapClient#searchRegionCode", response);

        assertThat(exception).isInstanceOfSatisfying(ApiException.class, apiException -> {
            assertThat(apiException.errorType()).isEqualTo(ErrorType.EXTERNAL_API_ERROR);
            assertThat(apiException.httpStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);
        });
    }

    @ParameterizedTest
    @ValueSource(ints = {599})
    void 알_수_없는_HTTP_상태는_BAD_GATEWAY로_변환한다(final int status) {
        final Response response = Response.builder()
                .status(status)
                .request(Request.create(
                        Request.HttpMethod.GET,
                        "https://dapi.kakao.com",
                        Map.of(),
                        (Request.Body) null,
                        null))
                .build();

        final Exception exception = new KakaoErrorDecoder()
                .decode("KakaoMapClient#searchRegionCode", response);

        assertThat(exception).isInstanceOfSatisfying(ApiException.class, apiException ->
                assertThat(apiException.httpStatus()).isEqualTo(HttpStatus.BAD_GATEWAY));
    }
}
