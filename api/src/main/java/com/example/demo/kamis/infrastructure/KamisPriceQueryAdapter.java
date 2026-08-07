package com.example.demo.kamis.infrastructure;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import com.example.demo.external.kamis.DailyPriceResponse;
import com.example.demo.external.kamis.Item;
import com.example.demo.external.kamis.feign.KamisClient;
import com.example.demo.kamis.application.port.KamisPriceQueryPort;
import com.example.demo.kamis.application.query.KamisDailyPriceQuery;
import com.example.demo.kamis.application.result.KamisDailyPriceItemResult;
import com.example.demo.kamis.application.result.KamisDailyPriceResult;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Slf4j
@Component("kamisPriceQueryPort")
@RequiredArgsConstructor
final class KamisPriceQueryAdapter implements KamisPriceQueryPort {

    private static final String DAILY_PRICE_BY_CATEGORY_LIST_ACTION = "dailyPriceByCategoryList";
    private static final String JSON_RETURN_TYPE = "json";
    private static final String SUCCESS_ERROR_CODE = "000";

    private final KamisClient kamisClient;

    @Override
    public KamisDailyPriceResult findDailyPrices(final KamisDailyPriceQuery query) {
        try {
            final DailyPriceResponse response = kamisClient.getDailyPrices(
                            DAILY_PRICE_BY_CATEGORY_LIST_ACTION,
                            query.productClsCode(),
                            query.itemCategoryCode(),
                            query.countryCode(),
                            toRegDay(query),
                            query.convertKgYn(),
                            JSON_RETURN_TYPE);
            if (!SUCCESS_ERROR_CODE.equals(response.errorCode())) {
                throw externalApiException();
            }
            return new KamisDailyPriceResult(response.errorCode(), null, toItems(response.items()));
        } catch (final ApiException | NullPointerException exception) {
            log.error("[KAMIS] response mapping failed errorMessage={}", exception.getMessage(), exception);
            if (exception instanceof ApiException apiException) {
                throw apiException;
            }
            throw externalApiException();
        }
    }

    private ApiException externalApiException() {
        return new ApiException(
                ErrorType.EXTERNAL_API_ERROR.description(),
                ErrorType.EXTERNAL_API_ERROR,
                HttpStatus.BAD_GATEWAY);
    }

    private List<KamisDailyPriceItemResult> toItems(final List<Item> items) {
        return items.stream()
                .map(this::toResult)
                .toList();
    }

    private String toRegDay(final KamisDailyPriceQuery query) {
        if (query.regDay() == null) {
            return null;
        }
        return query.regDay().toString();
    }

    private KamisDailyPriceItemResult toResult(final Item item) {
        return new KamisDailyPriceItemResult(
                firstNonBlank(item.itemName(), item.corpGdsItemNm()),
                firstNonBlank(item.itemCode(), item.corpGdsCd()),
                firstNonBlank(item.kindName(), item.corpGdsVrtyNm()),
                firstNonBlank(item.kindCode(), item.gdsSclsfCd()),
                item.rank(),
                unit(item),
                firstNonBlank(item.day1(), item.scsbdDt()),
                firstNonBlank(item.dpr1(), item.scsbdPrc()),
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

    private String unit(final Item item) {
        if (item.unit() != null && !item.unit().isBlank()) {
            return item.unit();
        }
        if (item.unitQty() == null || item.unitQty().isBlank()) {
            return item.unitNm();
        }
        if (item.unitNm() == null || item.unitNm().isBlank()) {
            return item.unitQty();
        }
        return item.unitQty() + item.unitNm();
    }

    private String firstNonBlank(final String preferred, final String fallback) {
        if (preferred != null && !preferred.isBlank()) {
            return preferred;
        }
        return fallback;
    }
}
