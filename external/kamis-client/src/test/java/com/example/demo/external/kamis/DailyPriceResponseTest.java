package com.example.demo.external.kamis;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class DailyPriceResponseTest {

    @Test
    void KAMIS_일별가격_응답을_Item과_Meta로_변환한다() throws IOException {
        final DailyPriceResponse response = new ObjectMapper().readValue(
                "{\"error_code\":\"000\",\"item\":[{"
                        + "\"item_name\":\"배추\",\"item_code\":\"211\","
                        + "\"kind_name\":\"여름 배추\",\"kind_code\":\"02\","
                        + "\"rank\":\"상품\",\"unit\":\"10kg\","
                        + "\"day1\":\"당일 (10/01)\",\"dpr1\":\"5,500\","
                        + "\"day7\":\"일평년\",\"dpr7\":\"8,751\"}],"
                        + "\"meta\":{\"dataType\":\"JSON\",\"numOfRows\":1,"
                        + "\"pageNo\":1,\"totalCount\":1}}",
                DailyPriceResponse.class);

        assertThat(response.errorCode()).isEqualTo("000");
        assertThat(response.items()).singleElement().satisfies(item -> {
            assertThat(item.itemName()).isEqualTo("배추");
            assertThat(item.itemCode()).isEqualTo("211");
            assertThat(item.day1()).isEqualTo("당일 (10/01)");
            assertThat(item.dpr1()).isEqualTo("5,500");
            assertThat(item.day7()).isEqualTo("일평년");
            assertThat(item.dpr7()).isEqualTo("8,751");
        });
        assertThat(response.meta().dataType()).isEqualTo("JSON");
        assertThat(response.meta().numOfRows()).isEqualTo(1);
        assertThat(response.meta().pageNo()).isEqualTo(1);
        assertThat(response.meta().totalCount()).isEqualTo(1);
    }

    @Test
    void KAMIS_응답에_item이_없으면_빈_리스트를_반환한다() throws IOException {
        final DailyPriceResponse response = new ObjectMapper().readValue(
                "{\"error_code\":\"000\"}",
                DailyPriceResponse.class);

        assertThat(response.items()).isEmpty();
    }
}
