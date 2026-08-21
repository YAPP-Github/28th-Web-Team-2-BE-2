package com.example.demo.external.kamis.feign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.demo.external.kamis.DailyPriceResponse;
import com.example.demo.external.kamis.WholesalePeriodPriceResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Request;
import feign.Response;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;

class KamisResponseDecoderTest {

    @Test
    void 실제_KAMIS_data_래퍼를_flat_response로_변환한다() throws Exception {
        final Response.Body body = mock(Response.Body.class);
        when(body.asInputStream()).thenReturn(new ByteArrayInputStream(
                ("{\"condition\":[],\"data\":{\"error_code\":\"000\",\"item\":[{"
                        + "\"item_name\":\"배추\",\"item_code\":\"211\"}]}}")
                        .getBytes(StandardCharsets.UTF_8)));
        final Response response = Response.builder()
                .status(200)
                .request(Request.create(
                        Request.HttpMethod.GET, "http://kamis.test", Map.of(), (Request.Body) null, null))
                .body(body)
                .build();

        final DailyPriceResponse decoded = (DailyPriceResponse) new KamisResponseDecoder(new ObjectMapper())
                .decode(response, DailyPriceResponse.class);

        assertThat(decoded.errorCode()).isEqualTo("000");
        assertThat(decoded.items()).singleElement().satisfies(item -> {
            assertThat(item.itemName()).isEqualTo("배추");
            assertThat(item.itemCode()).isEqualTo("211");
        });
    }

    @Test
    void 최상위_단일_배열로_감싼_KAMIS_data_응답을_flat_response로_변환한다() throws Exception {
        final Response response = Response.builder()
                .status(200)
                .request(Request.create(
                        Request.HttpMethod.GET, "http://kamis.test", Map.of(), (Request.Body) null, null))
                .body("[{\"condition\":[],\"data\":{\"error_code\":\"000\",\"item\":[{"
                        + "\"item_name\":\"배추\",\"item_code\":\"211\"}]}}]", StandardCharsets.UTF_8)
                .build();

        final DailyPriceResponse decoded = (DailyPriceResponse) new KamisResponseDecoder(new ObjectMapper())
                .decode(response, DailyPriceResponse.class);

        assertThat(decoded.errorCode()).isEqualTo("000");
        assertThat(decoded.items()).singleElement().satisfies(item -> {
            assertThat(item.itemName()).isEqualTo("배추");
            assertThat(item.itemCode()).isEqualTo("211");
        });
    }

    @Test
    void 기존_response_body_래퍼도_flat_response로_변환한다() throws Exception {
        final Response.Body body = mock(Response.Body.class);
        when(body.asInputStream()).thenReturn(new ByteArrayInputStream(
                ("{\"response\":{\"header\":{\"resultCode\":\"000\"},"
                        + "\"body\":{\"items\":{\"item\":[{"
                        + "\"corp_gds_item_nm\":\"양파\"}]},\"dataType\":\"JSON\","
                        + "\"numOfRows\":1,\"pageNo\":1,\"totalCount\":1}}}")
                        .getBytes(StandardCharsets.UTF_8)));
        final Response response = Response.builder()
                .status(200)
                .request(Request.create(
                        Request.HttpMethod.GET, "http://kamis.test", Map.of(), (Request.Body) null, null))
                .body(body)
                .build();

        final DailyPriceResponse decoded = (DailyPriceResponse) new KamisResponseDecoder(new ObjectMapper())
                .decode(response, DailyPriceResponse.class);

        assertThat(decoded.errorCode()).isEqualTo("000");
        assertThat(decoded.items()).singleElement()
                .satisfies(item -> assertThat(item.corpGdsItemNm()).isEqualTo("양파"));
        assertThat(decoded.meta().dataType()).isEqualTo("JSON");
        assertThat(decoded.meta().totalCount()).isEqualTo(1);
    }

    @Test
    void 도매_기간가격_data_응답을_기간가격_응답으로_변환한다() throws Exception {
        final Response.Body body = mock(Response.Body.class);
        when(body.asInputStream()).thenReturn(new ByteArrayInputStream(
                ("{\"data\":{\"error_code\":\"000\",\"item\":[{"
                        + "\"itemname\":\"고춧가루\",\"kindname\":\"국산\","
                        + "\"countyname\":\"서울\",\"marketname\":\"시장\","
                        + "\"yyyy\":\"2026\",\"regday\":\"08/19\","
                        + "\"price\":\"20,000\"}]}}")
                        .getBytes(StandardCharsets.UTF_8)));
        final Response response = Response.builder()
                .status(200)
                .request(Request.create(
                        Request.HttpMethod.GET, "http://kamis.test", Map.of(), (Request.Body) null, null))
                .body(body)
                .build();

        final WholesalePeriodPriceResponse decoded = (WholesalePeriodPriceResponse)
                new KamisResponseDecoder(new ObjectMapper()).decode(response, WholesalePeriodPriceResponse.class);

        assertThat(decoded.errorCode()).isEqualTo("000");
        assertThat(decoded.items()).singleElement().satisfies(item -> {
            assertThat(item.itemName()).isEqualTo("고춧가루");
            assertThat(item.kindName()).isEqualTo("국산");
            assertThat(item.regDay()).isEqualTo("08/19");
            assertThat(item.price()).isEqualTo("20,000");
        });
    }
}
