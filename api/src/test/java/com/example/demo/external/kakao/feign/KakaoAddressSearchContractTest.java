package com.example.demo.external.kakao.feign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Request;
import feign.Response;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
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
    void Kakao_주소_응답의_선행_0이_있는_b_code를_String으로_보존한다() throws Exception {
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
        final Object address = addresses.getFirst().getClass().getMethod("address").invoke(addresses.getFirst());

        assertThat(address.getClass().getMethod("bCode").invoke(address))
                .isEqualTo("0111010100")
                .isInstanceOf(String.class);
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
    void 빈_법정동_코드는_EXTERNAL_API_ERROR로_변환한다() {
        final String body = "{\"meta\":{\"total_count\":1},\"documents\":[{"
                + "\"address_name\":\"서울특별시 종로구 청운동\","
                + "\"address_type\":\"REGION\","
                + "\"address\":{\"address_name\":\"서울특별시 종로구 청운동\","
                + "\"region_1depth_name\":\"서울특별시\","
                + "\"region_2depth_name\":\"종로구\","
                + "\"region_3depth_name\":\"청운동\",\"b_code\":\"\"}}]}";

        assertThatThrownBy(() -> decode(body))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.errorType()).isEqualTo(ErrorType.EXTERNAL_API_ERROR));
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
