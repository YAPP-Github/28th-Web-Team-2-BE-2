package com.example.demo.external.kamis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class DefaultKamisClientTest {

    @Test
    void KAMIS_요청_파라미터와_외부_응답을_매핑한다() {
        final RestClient.Builder restClientBuilder = RestClient.builder().baseUrl("http://kamis.test");
        final MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        final RestClient restClient = restClientBuilder.build();
        final DefaultKamisClient client = new DefaultKamisClient(
                restClient,
                new KamisCredentials("cert-key-for-test", "9220"));

        server.expect(queryParam("action", "dailyPriceByCategoryList"))
                .andExpect(queryParam("p_product_cls_code", "02"))
                .andExpect(queryParam("p_item_category_code", "200"))
                .andExpect(queryParam("p_country_code", "1101"))
                .andExpect(queryParam("p_regday", "2015-10-01"))
                .andExpect(queryParam("p_convert_kg_yn", "N"))
                .andExpect(queryParam("p_cert_key", "cert-key-for-test"))
                .andExpect(queryParam("p_cert_id", "9220"))
                .andExpect(queryParam("p_returntype", "json"))
                .andRespond(withSuccess(
                        """
                        {
                          "data": {
                            "error_code": "000",
                            "error_msg": "Success.",
                            "item": [{
                              "item_name": "양파",
                              "itemcode": "211",
                              "kind_name": "양파",
                              "kindcode": "01",
                              "rank": "상품",
                              "unit": "1kg",
                              "day1": "2026-08-06",
                              "dpr1": "3,000"
                            }]
                          }
                        }
                        """,
                        MediaType.APPLICATION_JSON));

        final KamisDailyPriceResponse response = client.getDailyPrices(new KamisDailyPriceRequest(
                "02", "200", "1101", LocalDate.of(2015, 10, 1), "N"));

        server.verify();
        assertThat(response.errorCode()).isEqualTo("000");
        assertThat(response.errorMessage()).isEqualTo("Success.");
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().itemName()).isEqualTo("양파");
        assertThat(response.items().getFirst().dpr1()).isEqualTo("3,000");
    }
}
