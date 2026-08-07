package com.example.demo.kamis.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.external.kamis.KamisClient;
import com.example.demo.external.kamis.KamisDailyPriceItem;
import com.example.demo.external.kamis.KamisDailyPriceRequest;
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
        final AtomicReference<KamisDailyPriceRequest> capturedRequest = new AtomicReference<>();
        final KamisClient client = request -> {
            capturedRequest.set(request);
            return new KamisDailyPriceResponse(
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
                            null)));
        };
        final KamisPriceQueryAdapter adapter = new KamisPriceQueryAdapter(client);

        final KamisDailyPriceResult result = adapter.findDailyPrices(new KamisDailyPriceQuery(
                "02", "200", "1101", LocalDate.of(2015, 10, 1), "N"));

        assertThat(capturedRequest.get().productClsCode()).isEqualTo("02");
        assertThat(capturedRequest.get().itemCategoryCode()).isEqualTo("200");
        assertThat(capturedRequest.get().countryCode()).isEqualTo("1101");
        assertThat(capturedRequest.get().regDay()).isEqualTo(LocalDate.of(2015, 10, 1));
        assertThat(capturedRequest.get().convertKgYn()).isEqualTo("N");
        assertThat(result.errorCode()).isEqualTo("000");
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().getFirst().itemName()).isEqualTo("양파");
        assertThat(result.items().getFirst().dpr1()).isEqualTo("3,000");
    }
}
