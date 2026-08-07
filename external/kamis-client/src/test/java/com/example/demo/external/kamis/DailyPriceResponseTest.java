package com.example.demo.external.kamis;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class DailyPriceResponseTest {

    @Test
    void 실제_KAMIS_일별가격_응답을_data와_Item으로_변환한다() throws IOException {
        final DailyPriceResponse response = new ObjectMapper().readValue(
                "{\"condition\":[],\"data\":{\"error_code\":\"000\",\"item\":[{"
                        + "\"item_name\":\"배추\",\"item_code\":\"211\","
                        + "\"kind_name\":\"여름 배추\",\"kind_code\":\"02\","
                        + "\"rank\":\"상품\",\"unit\":\"10kg\","
                        + "\"day1\":\"당일 (10/01)\",\"dpr1\":\"5,500\","
                        + "\"day7\":\"일평년\",\"dpr7\":\"8,751\"}]}}",
                DailyPriceResponse.class);

        assertThat(response.data().errorCode()).isEqualTo("000");
        assertThat(response.data().items()).singleElement().satisfies(item -> {
            assertThat(item.itemName()).isEqualTo("배추");
            assertThat(item.itemCode()).isEqualTo("211");
            assertThat(item.day1()).isEqualTo("당일 (10/01)");
            assertThat(item.dpr1()).isEqualTo("5,500");
            assertThat(item.day7()).isEqualTo("일평년");
            assertThat(item.dpr7()).isEqualTo("8,751");
        });
    }

    @Test
    void KAMIS_정상_응답을_Item과_Meta로_변환한다() throws IOException {
        final DailyPriceResponse response = new ObjectMapper().readValue(
                "{\"response\":{\"header\":{\"resultCode\":\"000\",\"resultMsg\":\"정상\"},"
                        + "\"body\":{\"items\":{\"item\":[{"
                        + "\"auctn_seq\":\"1\",\"corp_gds_item_nm\":\"양파\",\"scsbd_prc\":\"3000\""
                        + "}]},\"dataType\":\"JSON\",\"numOfRows\":1,\"pageNo\":1,\"totalCount\":1}}}",
                DailyPriceResponse.class);

        assertThat(response.response().header().resultCode()).isEqualTo("000");
        assertThat(response.response().body().items().items()).singleElement()
                .satisfies(item -> {
                    assertThat(item.corpGdsItemNm()).isEqualTo("양파");
                    assertThat(item.scsbdPrc()).isEqualTo("3000");
                });
        assertThat(response.response().body().meta().dataType()).isEqualTo("JSON");
        assertThat(response.response().body().meta().numOfRows()).isEqualTo(1);
        assertThat(response.response().body().meta().pageNo()).isEqualTo(1);
        assertThat(response.response().body().meta().totalCount()).isEqualTo(1);
    }

    @Test
    void KAMIS_정상_응답에_item이_없으면_빈_리스트를_반환한다() throws IOException {
        final DailyPriceResponse response = new ObjectMapper().readValue(
                "{\"response\":{\"body\":{\"items\":{},\"dataType\":\"JSON\"}}}",
                DailyPriceResponse.class);

        assertThat(response.response().body().items().items()).isEmpty();
    }
}
