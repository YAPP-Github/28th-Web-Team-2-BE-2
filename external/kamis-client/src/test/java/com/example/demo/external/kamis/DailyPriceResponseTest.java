package com.example.demo.external.kamis;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class DailyPriceResponseTest {

    @Test
    void KAMIS_정상_응답을_Item과_Meta로_변환한다() throws IOException {
        final DailyPriceResponse response = new ObjectMapper().readValue(
                "{\"response\":{\"header\":{\"resultCode\":\"00\",\"resultMsg\":\"정상\"},"
                        + "\"body\":{\"items\":{\"item\":[{"
                        + "\"auctn_seq\":\"1\",\"corp_gds_item_nm\":\"양파\",\"scsbd_prc\":\"3000\""
                        + "}]},\"dataType\":\"JSON\",\"numOfRows\":1,\"pageNo\":1,\"totalCount\":1}}}",
                DailyPriceResponse.class);

        assertThat(response.response().header().resultCode()).isEqualTo("00");
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
