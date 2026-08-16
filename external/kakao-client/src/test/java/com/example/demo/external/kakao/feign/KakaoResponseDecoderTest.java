package com.example.demo.external.kakao.feign;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.external.kakao.KakaoCategorySearchResult;
import com.example.demo.external.kakao.KakaoRegionCodeResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Request;
import feign.Response;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;

class KakaoResponseDecoderTest {

    private final KakaoResponseDecoder decoder = new KakaoResponseDecoder(new ObjectMapper());

    @Test
    void 지역_코드_응답을_결과로_변환한다() throws Exception {
        final var result = decode(
                "{\"meta\":{\"total_count\":2},\"documents\":["
                        + "{\"region_type\":\"B\",\"code\":\"4413310500\","
                        + "\"region_2depth_name\":\"천안시 서북구\",\"region_3depth_name\":\"성성동\"},"
                        + "{\"region_type\":\"H\",\"code\":\"4413357000\","
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

    private <T> T decode(final String body, final Class<T> type) throws Exception {
        final Response response = Response.builder()
                .status(200)
                .reason("OK")
                .request(Request.create(
                        Request.HttpMethod.GET,
                        "https://dapi.kakao.com",
                        Map.of(),
                        (Request.Body) null,
                        StandardCharsets.UTF_8))
                .body(body, StandardCharsets.UTF_8)
                .build();
        return type.cast(decoder.decode(response, type));
    }
}
