package com.example.demo.kamis.infrastructure;

import com.example.demo.external.kamis.KamisClient;
import com.example.demo.external.kamis.KamisDailyPriceItem;
import com.example.demo.external.kamis.KamisDailyPriceData;
import com.example.demo.external.kamis.KamisDailyPriceResponse;
import com.example.demo.kamis.application.port.KamisPriceQueryPort;
import com.example.demo.kamis.application.query.KamisDailyPriceQuery;
import com.example.demo.kamis.application.result.KamisDailyPriceItemResult;
import com.example.demo.kamis.application.result.KamisDailyPriceResult;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component("kamisPriceQueryPort")
final class KamisPriceQueryAdapter implements KamisPriceQueryPort {

    private final KamisClient kamisClient;

    KamisPriceQueryAdapter(final KamisClient kamisClient) {
        this.kamisClient = kamisClient;
    }

    @Override
    public KamisDailyPriceResult findDailyPrices(final KamisDailyPriceQuery query) {
        final KamisDailyPriceResponse response = kamisClient.getDailyPrices(
                "dailyPriceByCategoryList",
                query.productClsCode(),
                query.itemCategoryCode(),
                query.countryCode(),
                toRegDay(query),
                query.convertKgYn(),
                "json");
        final KamisDailyPriceData data = Objects.requireNonNull(response.data());
        final List<KamisDailyPriceItemResult> items = data.items().stream()
                .map(this::toResult)
                .toList();
        return new KamisDailyPriceResult(data.errorCode(), data.errorMessage(), items);
    }

    private String toRegDay(final KamisDailyPriceQuery query) {
        if (query.regDay() == null) {
            return null;
        }
        return query.regDay().toString();
    }

    private KamisDailyPriceItemResult toResult(final KamisDailyPriceItem item) {
        return new KamisDailyPriceItemResult(
                item.itemName(),
                item.itemCode(),
                item.kindName(),
                item.kindCode(),
                item.rank(),
                item.unit(),
                item.day1(),
                item.dpr1(),
                item.day2(),
                item.dpr2(),
                item.day3(),
                item.dpr3(),
                item.day4(),
                item.dpr4(),
                item.day5(),
                item.dpr5(),
                item.day6(),
                item.dpr6(),
                item.day7(),
                item.dpr7());
    }
}
