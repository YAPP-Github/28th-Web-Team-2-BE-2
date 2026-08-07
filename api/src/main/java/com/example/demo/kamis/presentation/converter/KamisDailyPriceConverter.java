package com.example.demo.kamis.presentation.converter;

import com.example.demo.kamis.application.query.KamisDailyPriceQuery;
import com.example.demo.kamis.application.result.KamisDailyPriceItemResult;
import com.example.demo.kamis.application.result.KamisDailyPriceResult;
import com.example.demo.kamis.presentation.dto.KamisDailyPriceItemResponse;
import com.example.demo.kamis.presentation.dto.KamisDailyPriceRequest;
import com.example.demo.kamis.presentation.dto.KamisDailyPriceResponse;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class KamisDailyPriceConverter {

    public KamisDailyPriceQuery toQuery(final KamisDailyPriceRequest request) {
        return new KamisDailyPriceQuery(
                request.productClsCode(),
                request.itemCategoryCode(),
                request.countryCode(),
                request.regDay(),
                request.convertKgYn());
    }

    public KamisDailyPriceResponse toResponse(final KamisDailyPriceResult result) {
        final List<KamisDailyPriceItemResponse> items = result.items().stream()
                .map(this::toItemResponse)
                .toList();
        return new KamisDailyPriceResponse(result.errorCode(), result.errorMessage(), items);
    }

    private KamisDailyPriceItemResponse toItemResponse(final KamisDailyPriceItemResult item) {
        return new KamisDailyPriceItemResponse(
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
