package com.example.demo.external.kakao.feign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import com.example.demo.external.kakao.KakaoCategorySearchResult;
import com.example.demo.external.kakao.KakaoRegionCodeResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Request;
import feign.Response;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class KakaoResponseDecoderTest {

    private final KakaoResponseDecoder decoder = new KakaoResponseDecoder(new ObjectMapper());

    @Test
    void 지역_코드_응답을_결과로_변환한다() throws Exception {
        final var result = decode(
                "{\"meta\":{\"total_count\":2},\"documents\":["
                        + "{\"region_type\":\"B\",\"code\":\"4413310500\","
                        + "\"address_name\":\"충청남도 천안시 서북구 성성동\","
                        + "\"region_1depth_name\":\"충청남도\","
                        + "\"region_2depth_name\":\"천안시 서북구\",\"region_3depth_name\":\"성성동\"},"
                        + "{\"region_type\":\"H\",\"code\":\"4413357000\","
                        + "\"address_name\":\"충청남도 천안시 서북구 부성2동\","
                        + "\"region_1depth_name\":\"충청남도\","
                        + "\"region_2depth_name\":\"천안시 서북구\",\"region_3depth_name\":\"부성2동\"}]}",
                KakaoRegionCodeResult.class);

        assertThat(result.totalCount()).isEqualTo(2);
        assertThat(result.regions()).hasSize(2);
        assertThat(result.legalRegions()).hasSize(1);
    }

    @Test
    void 장소_검색_응답을_결과로_변환한다() throws Exception {
        final var result = decode(
                "{\"meta\":{\"total_count\":1},\"documents\":[{"
                        + "\"id\":\"123\",\"place_name\":\"강남마트\","
                        + "\"x\":\"127.0276\",\"y\":\"37.4979\","
                        + "\"address_name\":\"서울 강남구 삼성동 123\","
                        + "\"road_address_name\":\"서울 강남구 테헤란로 123\","
                        + "\"phone\":\"02-1234-5678\","
                        + "\"place_url\":\"http://place.map.kakao.com/123\","
                        + "\"category_group_code\":\"MT1\","
                        + "\"category_name\":\"가정,생활 > 슈퍼마켓\","
                        + "\"distance\":\"670\"}]}",
                KakaoCategorySearchResult.class);

        assertThat(result.totalCount()).isEqualTo(1);
        assertThat(result.places()).singleElement().satisfies(place -> {
            assertThat(place.placeName()).isEqualTo("강남마트");
            assertThat(place.latitude()).isEqualByComparingTo("37.4979");
            assertThat(place.longitude()).isEqualByComparingTo("127.0276");
            assertThat(place.distanceMeters()).isEqualTo(670);
        });
    }

    @Test
    void 잘못된_응답을_공통_API_예외로_변환한다() {
        assertThatThrownBy(() -> decode("{}", KakaoRegionCodeResult.class))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void documents가_누락된_지역_응답을_거부한다() {
        assertThatThrownBy(() -> decode(
                        "{\"meta\":{\"total_count\":0}}", KakaoRegionCodeResult.class))
                .isInstanceOfSatisfying(ApiException.class, this::assertExternalApiException);
    }

    @Test
    void documents가_null인_지역_응답을_거부한다() {
        assertThatThrownBy(() -> decode(
                        "{\"meta\":{\"total_count\":0},\"documents\":null}",
                        KakaoRegionCodeResult.class))
                .isInstanceOfSatisfying(ApiException.class, this::assertExternalApiException);
    }

    @Test
    void totalCount가_숫자가_아닌_응답을_거부한다() {
        assertThatThrownBy(() -> decode(
                        "{\"meta\":{\"total_count\":\"1\"},\"documents\":[]}",
                        KakaoRegionCodeResult.class))
                .isInstanceOfSatisfying(ApiException.class, this::assertExternalApiException);
    }

    @Test
    void 필수_필드가_누락된_지역_document를_거부한다() {
        assertThatThrownBy(() -> decode(
                        "{\"meta\":{\"total_count\":1},\"documents\":[{"
                                + "\"region_type\":\"B\",\"code\":\"4413310500\","
                                + "\"region_2depth_name\":\"천안시 서북구\"}]}",
                        KakaoRegionCodeResult.class))
                .isInstanceOfSatisfying(ApiException.class, this::assertExternalApiException);
    }

    @Test
    void 객체가_아닌_장소_document를_거부한다() {
        assertThatThrownBy(() -> decode(
                        "{\"meta\":{\"total_count\":1},\"documents\":[null]}",
                        KakaoCategorySearchResult.class))
                .isInstanceOfSatisfying(ApiException.class, this::assertExternalApiException);
    }

    private void assertExternalApiException(final ApiException exception) {
        assertThat(exception.errorType())
                .isEqualTo(ErrorType.EXTERNAL_API_ERROR);
        assertThat(exception.httpStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);
    }

    private <T> T decode(final String body, final Class<T> type) throws Exception {
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
        return type.cast(decoder.decode(response, type));
    }
}
