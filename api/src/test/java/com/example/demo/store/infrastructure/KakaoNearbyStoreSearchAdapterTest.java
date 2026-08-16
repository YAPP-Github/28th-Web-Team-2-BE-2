package com.example.demo.store.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.example.demo.store.application.query.NearbyStoreQuery;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class KakaoNearbyStoreSearchAdapterTest {

    @Test
    void Kakao_카테고리_검색에_경도_위도_반경을_매핑하고_응답_문서를_주변_매장으로_변환한다() {
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

        final KakaoNearbyStoreSearchAdapter adapter = new KakaoNearbyStoreSearchAdapter(builder, "rest-api-key");

        final var result = adapter.search(new NearbyStoreQuery(
                new BigDecimal("37.4979"), new BigDecimal("127.0276"), 1500));

        assertThat(result.totalCount()).isEqualTo(1);
        assertThat(result.stores())
                .singleElement()
                .satisfies(store -> {
                    assertThat(store.storeId()).isEqualTo("123");
                    assertThat(store.storeName()).isEqualTo("강남마트");
                    assertThat(store.latitude()).isEqualByComparingTo("37.4979");
                    assertThat(store.longitude()).isEqualByComparingTo("127.0276");
                    assertThat(store.addressName()).isEqualTo("서울 강남구 삼성동 123");
                    assertThat(store.roadAddressName()).isEqualTo("서울 강남구 테헤란로 123");
                    assertThat(store.phone()).isEqualTo("02-1234-5678");
                    assertThat(store.placeUrl()).isEqualTo("http://place.map.kakao.com/123");
                    assertThat(store.distanceMeters()).isEqualTo(670);
                    assertThat(store.isLiked()).isFalse();
                });
        server.verify();
    }
}
