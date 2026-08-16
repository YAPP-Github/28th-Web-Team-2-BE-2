package com.example.demo.external.kakao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class KakaoLocalRestClientTest {

    @Test
    void 카카오_카테고리_검색_요청과_응답을_변환한다() {
        final RestClient.Builder builder = RestClient.builder().baseUrl("https://dapi.kakao.com");
        final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(
                        "https://dapi.kakao.com/v2/local/search/category.json?category_group_code=MT1"
                                + "&x=127.0276&y=37.4979&radius=1500&sort=distance&size=15"))
                .andExpect(method(GET))
                .andExpect(header("Authorization", "KakaoAK rest-api-key"))
                .andRespond(withSuccess(
                        "{\"meta\":{\"total_count\":1},\"documents\":[{\"id\":\"123\",\"place_name\":\"강남마트\","
                                + "\"x\":\"127.0276\",\"y\":\"37.4979\","
                                + "\"address_name\":\"서울 강남구 삼성동 123\","
                                + "\"road_address_name\":\"서울 강남구 테헤란로 123\","
                                + "\"phone\":\"02-1234-5678\","
                                + "\"place_url\":\"http://place.map.kakao.com/123\","
                                + "\"distance\":\"670\"}]}",
                        MediaType.APPLICATION_JSON));

        final KakaoLocalRestClient client = new KakaoLocalRestClient(builder, "rest-api-key");

        final var result = client.searchCategory(new KakaoCategorySearchQuery(
                new BigDecimal("37.4979"), new BigDecimal("127.0276"), 1500));

        assertThat(result.totalCount()).isEqualTo(1);
        assertThat(result.places()).singleElement().satisfies(place -> {
            assertThat(place.id()).isEqualTo("123");
            assertThat(place.latitude()).isEqualByComparingTo("37.4979");
            assertThat(place.longitude()).isEqualByComparingTo("127.0276");
            assertThat(place.distanceMeters()).isEqualTo(670);
        });
        server.verify();
    }

    @Test
    void 카카오_좌표_행정구역_검색_요청과_응답을_변환한다() {
        final RestClient.Builder builder = RestClient.builder().baseUrl("https://dapi.kakao.com");
        final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(
                        "https://dapi.kakao.com/v2/local/geo/coord2regioncode.json"
                                + "?x=127.1324&y=36.8358"))
                .andExpect(method(GET))
                .andExpect(header("Authorization", "KakaoAK rest-api-key"))
                .andRespond(withSuccess(
                        "{\"meta\":{\"total_count\":2},\"documents\":["
                                + "{\"region_type\":\"B\",\"code\":\"4413310500\","
                                + "\"region_2depth_name\":\"천안시 서북구\","
                                + "\"region_3depth_name\":\"성성동\"},"
                                + "{\"region_type\":\"H\",\"code\":\"4413357000\","
                                + "\"region_2depth_name\":\"천안시 서북구\","
                                + "\"region_3depth_name\":\"부성2동\"}]}" ,
                        MediaType.APPLICATION_JSON));

        final KakaoLocalRestClient client = new KakaoLocalRestClient(builder, "rest-api-key");

        final var result = client.searchRegionCode(new KakaoRegionCodeQuery(
                new BigDecimal("36.8358"), new BigDecimal("127.1324")));

        assertThat(result.regions()).hasSize(2);
        assertThat(result.regions()).first().satisfies(region -> {
            assertThat(region.regionType()).isEqualTo("B");
            assertThat(region.code()).isEqualTo(4413310500L);
            assertThat(region.region2DepthName()).isEqualTo("천안시 서북구");
            assertThat(region.region3DepthName()).isEqualTo("성성동");
        });
        server.verify();
    }

    @Test
    void 카카오_좌표_행정구역_응답이_잘못되면_클라이언트_오류를_던진다() {
        final RestClient.Builder builder = RestClient.builder().baseUrl("https://dapi.kakao.com");
        final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(
                        "https://dapi.kakao.com/v2/local/geo/coord2regioncode.json"
                                + "?x=127.1324&y=36.8358"))
                .andRespond(withSuccess(
                        "{\"meta\":{\"total_count\":1},\"documents\":["
                                + "{\"region_type\":\"B\",\"code\":null,"
                                + "\"region_2depth_name\":\"천안시 서북구\","
                                + "\"region_3depth_name\":\"성성동\"}]}" ,
                        MediaType.APPLICATION_JSON));

        final KakaoLocalRestClient client = new KakaoLocalRestClient(builder, "rest-api-key");

        assertThatThrownBy(() -> client.searchRegionCode(new KakaoRegionCodeQuery(
                        new BigDecimal("36.8358"), new BigDecimal("127.1324"))))
                .isInstanceOf(KakaoClientException.class);
        server.verify();
    }
}
