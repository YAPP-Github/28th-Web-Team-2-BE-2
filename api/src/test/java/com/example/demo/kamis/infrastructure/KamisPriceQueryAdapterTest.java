package com.example.demo.kamis.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import com.example.demo.external.kamis.DailyPriceResponse;
import com.example.demo.external.kamis.Item;
import com.example.demo.external.kamis.feign.KamisClient;
import com.example.demo.kamis.application.query.KamisDailyPriceQuery;
import com.example.demo.kamis.application.result.KamisDailyPriceResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class KamisPriceQueryAdapterTest {

    @Test
    void 실제_KAMIS_응답을_일별가격_결과로_변환한다() throws IOException {
        final String json = """
                {
                  "error_code": "000",
                  "item": [{
                      "item_name": "배추",
                      "item_code": "211",
                      "kind_name": "여름(고랭지)(10kg)",
                      "kind_code": "02",
                      "rank": "상품",
                      "unit": "10kg",
                      "day1": "당일 (10/01)",
                      "dpr1": "5,500",
                      "day2": "1일전 (09/30)",
                      "dpr2": "7,000",
                      "day3": "1주일전 (09/24)",
                      "dpr3": "7,000",
                      "day4": "2주일전 (09/17)",
                      "dpr4": "6,500",
                      "day5": "1개월전",
                      "dpr5": "6,075",
                      "day6": "1년전",
                      "dpr6": "5,817",
                      "day7": "일평년",
                      "dpr7": "8,751"
                  }]
                }
                """;
        final DailyPriceResponse response = new ObjectMapper().readValue(json, DailyPriceResponse.class);
        final KamisPriceQueryAdapter adapter = new KamisPriceQueryAdapter(ignoredClient(response));

        final KamisDailyPriceResult result = adapter.findDailyPrices(query());

        assertThat(result.errorCode()).isEqualTo("000");
        assertThat(result.items()).singleElement().satisfies(item -> {
            assertThat(item.itemName()).isEqualTo("배추");
            assertThat(item.itemCode()).isEqualTo("211");
            assertThat(item.day1()).isEqualTo("당일 (10/01)");
            assertThat(item.dpr1()).isEqualTo("5,500");
            assertThat(item.day2()).isEqualTo("1일전 (09/30)");
            assertThat(item.dpr2()).isEqualTo("7,000");
            assertThat(item.day3()).isEqualTo("1주일전 (09/24)");
            assertThat(item.dpr3()).isEqualTo("7,000");
            assertThat(item.day4()).isEqualTo("2주일전 (09/17)");
            assertThat(item.dpr4()).isEqualTo("6,500");
            assertThat(item.day5()).isEqualTo("1개월전");
            assertThat(item.dpr5()).isEqualTo("6,075");
            assertThat(item.day6()).isEqualTo("1년전");
            assertThat(item.dpr6()).isEqualTo("5,817");
            assertThat(item.day7()).isEqualTo("일평년");
            assertThat(item.dpr7()).isEqualTo("8,751");
        });
    }

    @Test
    void 실제_KAMIS_data_오류_코드를_외부_연동_예외로_변환한다() {
        final DailyPriceResponse response = response("100", List.of());
        final KamisPriceQueryAdapter adapter = new KamisPriceQueryAdapter(ignoredClient(response));

        assertThatThrownBy(() -> adapter.findDailyPrices(query()))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.errorType()).isEqualTo(ErrorType.EXTERNAL_API_ERROR);
                    assertThat(exception.httpStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);
                    assertThat(exception.errorMessage()).isEqualTo(ErrorType.EXTERNAL_API_ERROR.description());
                });
    }

    @Test
    void 애플리케이션_조회와_외부_클라이언트_계약을_변환한다() {
        final AtomicReference<List<String>> capturedArguments = new AtomicReference<>();
        final KamisClient client = (action,
                                    productClsCode,
                                    itemCategoryCode,
                                    countryCode,
                                    regDay,
                                    convertKgYn,
                                    returnType) -> {
            capturedArguments.set(List.of(
                    action,
                    productClsCode,
                    itemCategoryCode,
                    countryCode,
                    regDay,
                    convertKgYn,
                    returnType));
            return response("000", List.of(item()));
        };
        final KamisPriceQueryAdapter adapter = new KamisPriceQueryAdapter(client);

        final KamisDailyPriceResult result = adapter.findDailyPrices(new KamisDailyPriceQuery(
                "02", "200", "1101", LocalDate.of(2015, 10, 1), "N"));

        assertThat(capturedArguments.get()).containsExactly(
                "dailyPriceByCategoryList", "02", "200", "1101", "2015-10-01", "N", "json");
        assertThat(result.errorCode()).isEqualTo("000");
        assertThat(result.errorMessage()).isNull();
        assertThat(result.items()).singleElement().satisfies(mappedItem -> {
            assertThat(mappedItem.itemName()).isEqualTo("양파");
            assertThat(mappedItem.itemCode()).isEqualTo("211");
            assertThat(mappedItem.kindName()).isEqualTo("양파");
            assertThat(mappedItem.kindCode()).isEqualTo("010101");
            assertThat(mappedItem.unit()).isEqualTo("1kg");
            assertThat(mappedItem.day1()).isEqualTo("2026-08-06");
            assertThat(mappedItem.dpr1()).isEqualTo("3,000");
            assertThat(mappedItem.day2()).isEqualTo("2026-08-05");
            assertThat(mappedItem.dpr2()).isEqualTo("2,900");
            assertThat(mappedItem.day7()).isEqualTo("2021-08-06");
            assertThat(mappedItem.dpr7()).isEqualTo("2,400");
        });
    }

    @Test
    void 외부_응답이_null이면_외부_연동_예외를_던진다() {
        final KamisPriceQueryAdapter adapter = new KamisPriceQueryAdapter(ignoredClient(null));

        assertThatThrownBy(() -> adapter.findDailyPrices(query()))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.errorType()).isEqualTo(ErrorType.EXTERNAL_API_ERROR);
                    assertThat(exception.httpStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);
                    assertThat(exception.errorMessage()).isEqualTo(ErrorType.EXTERNAL_API_ERROR.description());
                });
    }

    @Test
    void HTTP_200_응답의_KAMIS_오류_코드를_외부_연동_예외로_변환한다() {
        final KamisPriceQueryAdapter adapter = new KamisPriceQueryAdapter(
                ignoredClient(response("100", List.of())));

        assertThatThrownBy(() -> adapter.findDailyPrices(query()))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.errorType()).isEqualTo(ErrorType.EXTERNAL_API_ERROR);
                    assertThat(exception.httpStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);
                    assertThat(exception.errorMessage()).isEqualTo(ErrorType.EXTERNAL_API_ERROR.description());
                });
    }

    @Test
    void 실제_KAMIS_item이_null이면_빈_목록으로_변환한다() {
        final KamisPriceQueryAdapter adapter = new KamisPriceQueryAdapter(
                ignoredClient(response("000", null)));

        final KamisDailyPriceResult result = adapter.findDailyPrices(query());

        assertThat(result.items()).isEmpty();
    }

    private KamisClient ignoredClient(final DailyPriceResponse response) {
        return (action, productClsCode, itemCategoryCode, countryCode, regDay, convertKgYn, returnType) -> response;
    }

    private DailyPriceResponse response(
            final String errorCode,
            final List<Item> items) {
        return DailyPriceResponse.builder()
                .errorCode(errorCode)
                .items(items)
                .build();
    }

    private Item item() {
        return new Item(
                "1",
                "2026-08-06",
                "2026-08-06",
                "1101",
                "서울",
                "corp-code",
                "법인",
                "01",
                "식량작물",
                "0101",
                "채소",
                "010101",
                "양파",
                "211",
                "양파",
                "양파",
                "01",
                "국산",
                "3,000",
                "1",
                "1",
                "KG",
                "kg",
                "01",
                "포장",
                "sample",
                "경매",
                "양파",
                "211",
                "양파",
                "010101",
                "상품",
                "1kg",
                "2026-08-06",
                "3,000",
                "2026-08-05",
                "2,900",
                "2026-07-30",
                "2,800",
                "2026-07-23",
                "2,700",
                "2026-07-06",
                "2,600",
                "2025-08-06",
                "2,500",
                "2021-08-06",
                "2,400");
    }

    private KamisDailyPriceQuery query() {
        return new KamisDailyPriceQuery("02", "200", "1101", LocalDate.of(2015, 10, 1), "N");
    }
}
