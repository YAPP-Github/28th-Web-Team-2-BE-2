package com.example.demo.external.kakao.feign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import com.example.demo.external.kakao.KakaoAddressSearchResult;
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
                "{\"meta\":{\"total_count\":1,\"pageable_count\":1,\"is_end\":true},\"documents\":[{"
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
        assertThat(result.pageableCount()).isEqualTo(1);
        assertThat(result.end()).isTrue();
        assertThat(result.places()).singleElement().satisfies(place -> {
            assertThat(place.placeName()).isEqualTo("강남마트");
            assertThat(place.latitude()).isEqualByComparingTo("37.4979");
            assertThat(place.longitude()).isEqualByComparingTo("127.0276");
            assertThat(place.distanceMeters()).isEqualTo(670);
        });
    }

    @Test
    void 주소가_null이거나_법정동_이름이_비어있는_document는_제외한다() throws Exception {
        final var result = decode("""
                {"meta":{"total_count":3},"documents":[
                  {"address_name":"도로명 주소","address_type":"ROAD","x":"126.9707","y":"37.5874","address":null},
                  {"address_name":"빈 법정동 주소","address_type":"REGION","x":"126.9707","y":"37.5874",
                   "address":{"address_name":"빈 법정동 주소","region_1depth_name":"서울특별시",
                   "region_2depth_name":"종로구","region_3depth_name":"","b_code":"0111010100"}},
                  {"address_name":"서울특별시 종로구 청운동","address_type":"REGION",
                   "x":"126.9707","y":"37.5874",
                   "address":{"address_name":"서울특별시 종로구 청운동","region_1depth_name":"서울특별시",
                   "region_2depth_name":"종로구","region_3depth_name":"청운동","b_code":"0111010100"}}
                ]}
                """, KakaoAddressSearchResult.class);

        assertThat(result.addresses()).singleElement().satisfies(address -> {
            assertThat(address.addressName()).isEqualTo("서울특별시 종로구 청운동");
            assertThat(address.address().bCode()).isEqualTo("0111010100");
        });
    }

    @Test
    void 주소가_모두_제외되면_빈_결과를_반환한다() throws Exception {
        final var result = decode("""
                {"meta":{"total_count":2},"documents":[
                  {"address_name":"도로명 주소","address_type":"ROAD","x":"126.9707","y":"37.5874","address":null},
                  {"address_name":"빈 법정동 주소","address_type":"REGION","x":"126.9707","y":"37.5874",
                   "address":{"address_name":"빈 법정동 주소","region_1depth_name":"서울특별시",
                   "region_2depth_name":"종로구","region_3depth_name":"","b_code":"0111010100"}}
                ]}
                """, KakaoAddressSearchResult.class);

        assertThat(result.addresses()).isEmpty();
    }

    @Test
    void 법정동_이름이_비어있어도_필수_필드가_누락되면_거부한다() {
        assertThatThrownBy(() -> decode("""
                {"meta":{"total_count":1},"documents":[
                  {"address_name":"빈 법정동 주소","address_type":"REGION","x":"126.9707","y":"37.5874",
                   "address":{"region_3depth_name":""}}
                ]}
                """, KakaoAddressSearchResult.class))
                .isInstanceOfSatisfying(ApiException.class, this::assertExternalApiException);
    }

    @Test
    void 주소가_null이어도_좌표가_잘못되면_거부한다() {
        assertThatThrownBy(() -> decode("""
                {"meta":{"total_count":1},"documents":[
                  {"address_name":"도로명 주소","address_type":"ROAD","x":"181","y":"37.5874","address":null}
                ]}
                """, KakaoAddressSearchResult.class))
                .isInstanceOfSatisfying(ApiException.class, this::assertExternalApiException);
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

    @Test
    void 장소_검색의_pageable_count가_공식_상한을_초과하면_거부한다() {
        assertThatThrownBy(() -> decode(
                        "{\"meta\":{\"total_count\":46,\"pageable_count\":46,\"is_end\":true},"
                                + "\"documents\":[]}",
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
