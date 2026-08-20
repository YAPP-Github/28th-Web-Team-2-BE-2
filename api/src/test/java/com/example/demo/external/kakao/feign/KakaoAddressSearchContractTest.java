package com.example.demo.external.kakao.feign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Request;
import feign.Response;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

class KakaoAddressSearchContractTest {

    private final KakaoResponseDecoder decoder = new KakaoResponseDecoder(new ObjectMapper());

    @Test
    void Kakao_주소_검색_endpoint는_query를_받고_주소_검색_결과를_반환한다() throws Exception {
        final Method searchAddress = KakaoMapClient.class.getDeclaredMethod("searchAddress", String.class, int.class);
        final GetMapping getMapping = searchAddress.getAnnotation(GetMapping.class);
        final RequestParam requestParam = (RequestParam) searchAddress
                .getParameters()[0]
                .getAnnotation(RequestParam.class);

        assertThat(getMapping.value()).containsExactly("/v2/local/search/address.json");
        assertThat(requestParam.value()).isEqualTo("query");
        assertThat(searchAddress.getParameters()[1].getAnnotation(RequestParam.class).value()).isEqualTo("size");
        assertThat(searchAddress.getReturnType().getSimpleName()).isEqualTo("KakaoAddressSearchResult");
    }

    @Test
    void Kakao_주소_응답의_선행_0이_있는_b_code와_좌표를_보존한다() throws Exception {
        final Object result = decode(
                "{\"meta\":{\"total_count\":1},\"documents\":[{"
                        + "\"address_name\":\"서울특별시 종로구 청운동\","
                        + "\"address_type\":\"REGION\","
                        + "\"x\":\"126.9707\",\"y\":\"37.5874\","
                        + "\"address\":{\"address_name\":\"서울특별시 종로구 청운동\","
                        + "\"region_1depth_name\":\"서울특별시\","
                        + "\"region_2depth_name\":\"종로구\","
                        + "\"region_3depth_name\":\"청운동\","
                        + "\"b_code\":\"0111010100\"},"
                        + "\"road_address\":{\"address_name\":\"서울특별시 종로구 자하문로 1\"}}]}");

        final List<?> addresses = (List<?>) result.getClass().getMethod("addresses").invoke(result);
        final Object kakaoAddress = addresses.getFirst();
        final Object address = kakaoAddress.getClass().getMethod("address").invoke(kakaoAddress);

        assertThat(address.getClass().getMethod("bCode").invoke(address))
                .isEqualTo("0111010100")
                .isInstanceOf(String.class);
        assertThat(kakaoAddress.getClass().getMethod("longitude").invoke(kakaoAddress))
                .isEqualTo(new BigDecimal("126.9707"));
        assertThat(kakaoAddress.getClass().getMethod("latitude").invoke(kakaoAddress))
                .isEqualTo(new BigDecimal("37.5874"));
    }

    @Test
    void 잘못된_Kakao_주소_응답은_EXTERNAL_API_ERROR로_변환한다() throws Exception {
        assertThatThrownBy(() -> decode("{\"meta\":{\"total_count\":1},\"documents\":[{}]}"))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.errorType()).isEqualTo(ErrorType.EXTERNAL_API_ERROR);
                    assertThat(exception.httpStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);
                });
    }

    @Test
    void 빈_법정동_코드도_주소_응답으로_디코딩한다() throws Exception {
        final String body = "{\"meta\":{\"total_count\":1},\"documents\":[{"
                + "\"address_name\":\"서울특별시 종로구 청운동\","
                + "\"address_type\":\"REGION\","
                + "\"x\":\"126.9707\",\"y\":\"37.5874\","
                + "\"address\":{\"address_name\":\"서울특별시 종로구 청운동\","
                + "\"region_1depth_name\":\"서울특별시\","
                + "\"region_2depth_name\":\"종로구\","
                + "\"region_3depth_name\":\"청운동\",\"b_code\":\"\"}}]}";

        final Object result = decode(body);
        final List<?> addresses = (List<?>) result.getClass().getMethod("addresses").invoke(result);
        final Object address = addresses.getFirst().getClass().getMethod("address").invoke(addresses.getFirst());

        assertThat(address.getClass().getMethod("bCode").invoke(address)).isEqualTo("");
    }

    @Test
    void 좌표가_누락된_Kakao_주소_응답은_EXTERNAL_API_ERROR로_변환한다() {
        assertThatThrownBy(() -> decode(
                        "{\"meta\":{\"total_count\":1},\"documents\":[{"
                                + "\"address_name\":\"서울특별시 종로구 청운동\","
                                + "\"address_type\":\"REGION\","
                                + "\"address\":{\"address_name\":\"서울특별시 종로구 청운동\","
                                + "\"region_1depth_name\":\"서울특별시\","
                                + "\"region_2depth_name\":\"종로구\","
                                + "\"region_3depth_name\":\"청운동\","
                                + "\"b_code\":\"0111010100\"}}]}"))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.errorType()).isEqualTo(ErrorType.EXTERNAL_API_ERROR);
                    assertThat(exception.httpStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);
                });
    }

    @ParameterizedTest
    @CsvSource({"181, 37.5874", "126.9707, 91"})
    void 범위를_벗어난_Kakao_주소_좌표는_EXTERNAL_API_ERROR로_변환한다(
            final String longitude, final String latitude) {
        assertThatThrownBy(() -> decode(
                        "{\"meta\":{\"total_count\":1},\"documents\":[{"
                                + "\"address_name\":\"서울특별시 종로구 청운동\","
                                + "\"address_type\":\"REGION\","
                                + "\"x\":\"" + longitude + "\",\"y\":\"" + latitude + "\","
                                + "\"address\":{\"address_name\":\"서울특별시 종로구 청운동\","
                                + "\"region_1depth_name\":\"서울특별시\","
                                + "\"region_2depth_name\":\"종로구\","
                                + "\"region_3depth_name\":\"청운동\","
                                + "\"b_code\":\"0111010100\"}}]}"))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.errorType()).isEqualTo(ErrorType.EXTERNAL_API_ERROR);
                    assertThat(exception.httpStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);
                });
    }

    private Object decode(final String body) throws Exception {
        final Class<?> addressSearchResultType = Class.forName(
                "com.example.demo.external.kakao.KakaoAddressSearchResult");
        final Response response = Response.builder()
                .status(200)
                .reason("OK")
                .request(Request.create(
                        Request.HttpMethod.GET,
                        "https://dapi.kakao.com",
                        Map.of(),
                        (Request.Body) null,
                        null))
                .body(body, StandardCharsets.UTF_8)
                .build();
        return decoder.decode(response, addressSearchResultType);
    }
}
