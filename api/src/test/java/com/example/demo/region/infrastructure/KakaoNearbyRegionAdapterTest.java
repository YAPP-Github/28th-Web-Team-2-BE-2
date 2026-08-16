package com.example.demo.region.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.example.demo.common.exception.ApiException;
import com.example.demo.region.application.query.NearbyRegionQuery;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class KakaoNearbyRegionAdapterTest {

    @Test
    void Kakao_좌표_행정구역_검색에서_법정동만_변환한다() {
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
                                + "\"region_2depth_name\":\"천안시 서북구\",\"region_3depth_name\":\"성성동\"},"
                                + "{\"region_type\":\"H\",\"code\":\"4413357000\","
                                + "\"region_2depth_name\":\"천안시 서북구\",\"region_3depth_name\":\"부성2동\"}]}" ,
                        MediaType.APPLICATION_JSON));

        final KakaoNearbyRegionAdapter adapter = new KakaoNearbyRegionAdapter(builder, "rest-api-key");

        final var result = adapter.find(new NearbyRegionQuery(
                new BigDecimal("36.8358"), new BigDecimal("127.1324")));

        assertThat(result.regions())
                .singleElement()
                .satisfies(region -> {
                    assertThat(region.regionId()).isEqualTo("4413310500");
                    assertThat(region.regionName()).isEqualTo("천안시 서북구 성성동");
                });
        server.verify();
    }

    @Test
    void Kakao_응답_구조가_잘못되면_외부_API_오류로_변환한다() {
        final RestClient.Builder builder = RestClient.builder().baseUrl("https://dapi.kakao.com");
        final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(
                        "https://dapi.kakao.com/v2/local/geo/coord2regioncode.json"
                                + "?x=127.1324&y=36.8358"))
                .andRespond(withSuccess("{\"meta\":{\"total_count\":1}}", MediaType.APPLICATION_JSON));

        final KakaoNearbyRegionAdapter adapter = new KakaoNearbyRegionAdapter(builder, "rest-api-key");

        assertThatThrownBy(() -> adapter.find(new NearbyRegionQuery(
                        new BigDecimal("36.8358"), new BigDecimal("127.1324"))))
                .isInstanceOf(ApiException.class);
        server.verify();
    }
}
