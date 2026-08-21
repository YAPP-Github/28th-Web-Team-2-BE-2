package com.example.demo.kamis.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.external.kamis.DailyPriceResponse;
import com.example.demo.external.kamis.WholesalePeriodPriceItem;
import com.example.demo.external.kamis.WholesalePeriodPriceResponse;
import com.example.demo.external.kamis.feign.KamisClient;
import com.example.demo.kamis.application.query.KamisPeriodPriceQuery;
import com.example.demo.kamis.application.result.KamisPeriodPriceResult;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class KamisPeriodPriceQueryAdapterTest {

    @Test
    void 도매_기간가격_응답과_요청_파라미터를_애플리케이션_계약으로_변환한다() {
        final AtomicReference<List<String>> capturedArguments = new AtomicReference<>();
        final KamisClient client = new KamisClient() {
            @Override
            public DailyPriceResponse getDailyPrices(
                    final String action,
                    final String productClsCode,
                    final String itemCategoryCode,
                    final String countryCode,
                    final String regDay,
                    final String convertKgYn,
                    final String returnType) {
                return null;
            }

            @Override
            public WholesalePeriodPriceResponse getWholesalePeriodPrices(
                    final String action,
                    final String startDay,
                    final String endDay,
                    final String itemCategoryCode,
                    final String itemCode,
                    final String kindCode,
                    final String productRankCode,
                    final String countryCode,
                    final String convertKgYn,
                    final String returnType) {
                capturedArguments.set(List.of(
                        action,
                        startDay,
                        endDay,
                        itemCategoryCode,
                        itemCode,
                        kindCode,
                        productRankCode,
                        countryCode,
                        convertKgYn,
                        returnType));
                return new WholesalePeriodPriceResponse(
                        "000",
                        List.of(new WholesalePeriodPriceItem(
                                "고춧가루", "국산", "서울", "시장", "2026", "08/19", "20,000", "1kg")),
                        null);
            }
        };
        final KamisPriceQueryAdapter adapter = new KamisPriceQueryAdapter(client);

        final KamisPeriodPriceResult result = adapter.findWholesalePeriodPrices(new KamisPeriodPriceQuery(
                "200", "248", "01", "04", "1101",
                LocalDate.of(2025, 8, 21), LocalDate.of(2026, 8, 20), "Y"));

        assertThat(capturedArguments.get()).containsExactly(
                "periodWholesaleProductList",
                "2025-08-21",
                "2026-08-20",
                "200",
                "248",
                "01",
                "04",
                "1101",
                "Y",
                "json");
        assertThat(result.errorCode()).isEqualTo("000");
        assertThat(result.items()).singleElement().satisfies(item -> {
            assertThat(item.itemName()).isEqualTo("고춧가루");
            assertThat(item.kindName()).isEqualTo("국산");
            assertThat(item.countyName()).isEqualTo("서울");
            assertThat(item.regDay()).isEqualTo("08/19");
            assertThat(item.price()).isEqualTo("20,000");
        });
    }

    @Test
    void KAMIS_조회결과없음은_빈_기간가격_결과로_변환한다() {
        final KamisClient client = new KamisClient() {
            @Override
            public DailyPriceResponse getDailyPrices(
                    final String action,
                    final String productClsCode,
                    final String itemCategoryCode,
                    final String countryCode,
                    final String regDay,
                    final String convertKgYn,
                    final String returnType) {
                return null;
            }

            @Override
            public WholesalePeriodPriceResponse getWholesalePeriodPrices(
                    final String action,
                    final String startDay,
                    final String endDay,
                    final String itemCategoryCode,
                    final String itemCode,
                    final String kindCode,
                    final String productRankCode,
                    final String countryCode,
                    final String convertKgYn,
                    final String returnType) {
                return new WholesalePeriodPriceResponse("001", List.of(), null);
            }
        };
        final KamisPriceQueryAdapter adapter = new KamisPriceQueryAdapter(client);

        final KamisPeriodPriceResult result = adapter.findWholesalePeriodPrices(new KamisPeriodPriceQuery(
                "200", "248", "01", "04", "1101",
                LocalDate.of(2025, 8, 21), LocalDate.of(2026, 8, 20), "Y"));

        assertThat(result.errorCode()).isEqualTo("001");
        assertThat(result.items()).isEmpty();
    }
}
