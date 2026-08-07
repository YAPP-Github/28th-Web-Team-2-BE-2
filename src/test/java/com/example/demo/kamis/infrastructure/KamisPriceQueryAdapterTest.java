package com.example.demo.kamis.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.external.kamis.feign.KamisClient;
import com.example.demo.external.kamis.KamisDailyPriceData;
import com.example.demo.external.kamis.KamisDailyPriceItem;
import com.example.demo.external.kamis.KamisDailyPriceResponse;
import com.example.demo.kamis.application.query.KamisDailyPriceQuery;
import com.example.demo.kamis.application.result.KamisDailyPriceResult;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

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
            return new KamisDailyPriceResponse(new KamisDailyPriceData(
                    "000",
                    "Success.",
                    List.of(new KamisDailyPriceItem(
                            "양파",
                            "211",
                            "양파",
                            "01",
                            "상품",
                            "1kg",
                            "2026-08-06",
                            "3,000",
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null))));
        };
        final KamisPriceQueryAdapter adapter = new KamisPriceQueryAdapter(client);

        final KamisDailyPriceResult result = adapter.findDailyPrices(new KamisDailyPriceQuery(
                "02", "200", "1101", LocalDate.of(2015, 10, 1), "N"));

        assertThat(capturedArguments.get()).containsExactly(
                "dailyPriceByCategoryList", "02", "200", "1101", "2015-10-01", "N", "json");
        assertThat(result.errorCode()).isEqualTo("000");
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().getFirst().itemName()).isEqualTo("양파");
        assertThat(result.items().getFirst().dpr1()).isEqualTo("3,000");
    }
}
