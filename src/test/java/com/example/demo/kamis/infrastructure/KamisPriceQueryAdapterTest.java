package com.example.demo.kamis.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import com.example.demo.external.kamis.DailyPriceResponse;
import com.example.demo.external.kamis.Item;
import com.example.demo.external.kamis.Meta;
import com.example.demo.external.kamis.feign.KamisClient;
import com.example.demo.kamis.application.query.KamisDailyPriceQuery;
import com.example.demo.kamis.application.result.KamisDailyPriceResult;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class KamisPriceQueryAdapterTest {

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
            return response("000", "정상", List.of(item()));
        };
        final KamisPriceQueryAdapter adapter = new KamisPriceQueryAdapter(client);

        final KamisDailyPriceResult result = adapter.findDailyPrices(new KamisDailyPriceQuery(
                "02", "200", "1101", LocalDate.of(2015, 10, 1), "N"));

        assertThat(capturedArguments.get()).containsExactly(
                "dailyPriceByCategoryList", "02", "200", "1101", "2015-10-01", "N", "json");
        assertThat(result.errorCode()).isEqualTo("000");
        assertThat(result.errorMessage()).isEqualTo("정상");
        assertThat(result.items()).singleElement().satisfies(mappedItem -> {
            assertThat(mappedItem.itemName()).isEqualTo("양파");
            assertThat(mappedItem.itemCode()).isEqualTo("211");
            assertThat(mappedItem.kindName()).isEqualTo("양파");
            assertThat(mappedItem.kindCode()).isEqualTo("010101");
            assertThat(mappedItem.unit()).isEqualTo("1kg");
            assertThat(mappedItem.day1()).isEqualTo("2026-08-06");
            assertThat(mappedItem.dpr1()).isEqualTo("3,000");
            assertThat(mappedItem.day2()).isNull();
            assertThat(mappedItem.dpr2()).isNull();
        });
    }

    @Test
    void 외부_응답이_null이면_외부_연동_예외를_던진다() {
        final KamisPriceQueryAdapter adapter = new KamisPriceQueryAdapter(ignoredClient(null));

        assertThatThrownBy(() -> adapter.findDailyPrices(query()))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.errorType()).isEqualTo(ErrorType.EXTERNAL_API_ERROR);
                    assertThat(exception.httpStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);
                    assertThat(exception.errorMessage()).isEqualTo("KAMIS API 응답이 올바르지 않습니다.");
                });
    }

    @Test
    void 외부_응답의_response가_null이면_외부_연동_예외를_던진다() {
        final KamisPriceQueryAdapter adapter = new KamisPriceQueryAdapter(ignoredClient(new DailyPriceResponse(null)));

        assertThatThrownBy(() -> adapter.findDailyPrices(query()))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void HTTP_200_응답의_KAMIS_오류_코드를_외부_연동_예외로_변환한다() {
        final KamisPriceQueryAdapter adapter = new KamisPriceQueryAdapter(
                ignoredClient(response("100", "인증 정보가 올바르지 않습니다.", List.of())));

        assertThatThrownBy(() -> adapter.findDailyPrices(query()))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.errorType()).isEqualTo(ErrorType.EXTERNAL_API_ERROR);
                    assertThat(exception.httpStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);
                    assertThat(exception.errorMessage()).isEqualTo("인증 정보가 올바르지 않습니다.");
                });
    }

    @Test
    void 가격_항목이_null이면_빈_목록으로_변환한다() {
        final KamisPriceQueryAdapter adapter = new KamisPriceQueryAdapter(
                ignoredClient(response("000", "정상", null)));

        final KamisDailyPriceResult result = adapter.findDailyPrices(query());

        assertThat(result.items()).isEmpty();
    }

    private KamisClient ignoredClient(final DailyPriceResponse response) {
        return (action, productClsCode, itemCategoryCode, countryCode, regDay, convertKgYn, returnType) -> response;
    }

    private DailyPriceResponse response(
            final String resultCode,
            final String resultMsg,
            final List<Item> items) {
        return new DailyPriceResponse(new DailyPriceResponse.Response(
                new DailyPriceResponse.Header(resultCode, resultMsg),
                new DailyPriceResponse.Body(
                        new DailyPriceResponse.Items(items),
                        new Meta("JSON", 1, 1, totalCount(items)))));
    }

    private int totalCount(final List<Item> items) {
        if (items == null) {
            return 0;
        }
        return items.size();
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
                "경매");
    }

    private KamisDailyPriceQuery query() {
        return new KamisDailyPriceQuery("02", "200", "1101", LocalDate.of(2015, 10, 1), "N");
    }
}
