package com.example.demo.kamis.infrastructure;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import com.example.demo.external.kamis.feign.KamisClient;
import com.example.demo.external.kamis.KamisDailyPriceItem;
import com.example.demo.external.kamis.KamisDailyPriceData;
import com.example.demo.external.kamis.KamisDailyPriceResponse;
import com.example.demo.kamis.application.port.KamisPriceQueryPort;
import com.example.demo.kamis.application.query.KamisDailyPriceQuery;
import com.example.demo.kamis.application.result.KamisDailyPriceItemResult;
import com.example.demo.kamis.application.result.KamisDailyPriceResult;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component("kamisPriceQueryPort")
@RequiredArgsConstructor
final class KamisPriceQueryAdapter implements KamisPriceQueryPort {

    private static final String SUCCESS_ERROR_CODE = "000";
    private static final String DEFAULT_ERROR_MESSAGE = "KAMIS API 호출에 실패했습니다.";
    private static final String INVALID_RESPONSE_MESSAGE = "KAMIS API 응답이 올바르지 않습니다.";

    private final KamisClient kamisClient;

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
        if (response == null || response.data() == null) {
            throw new ApiException(INVALID_RESPONSE_MESSAGE, ErrorType.EXTERNAL_API_ERROR, HttpStatus.BAD_GATEWAY);
        }
        final KamisDailyPriceData data = response.data();
        if (!SUCCESS_ERROR_CODE.equals(data.errorCode())) {
            throw new ApiException(errorMessage(data), ErrorType.EXTERNAL_API_ERROR, HttpStatus.BAD_GATEWAY);
        }
        final List<KamisDailyPriceItemResult> items = data.items().stream()
                .map(this::toResult)
                .toList();
        return new KamisDailyPriceResult(data.errorCode(), data.errorMessage(), items);
    }

    private String errorMessage(final KamisDailyPriceData data) {
        if (data.errorMessage() == null || data.errorMessage().isBlank()) {
            return DEFAULT_ERROR_MESSAGE;
        }
        return data.errorMessage();
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
