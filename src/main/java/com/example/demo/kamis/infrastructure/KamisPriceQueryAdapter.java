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
        final DailyPriceResponse response = kamisClient.getDailyPrices(
                "dailyPriceByCategoryList",
                query.productClsCode(),
                query.itemCategoryCode(),
                query.countryCode(),
                toRegDay(query),
                query.convertKgYn(),
                "json");
        if (response == null || response.response() == null
                || response.response().header() == null
                || response.response().body() == null
                || response.response().body().items() == null) {
            throw new ApiException(INVALID_RESPONSE_MESSAGE, ErrorType.EXTERNAL_API_ERROR, HttpStatus.BAD_GATEWAY);
        }
        final DailyPriceResponse.Header header = response.response().header();
        if (!SUCCESS_ERROR_CODE.equals(header.resultCode())) {
            throw new ApiException(errorMessage(header), ErrorType.EXTERNAL_API_ERROR, HttpStatus.BAD_GATEWAY);
        }
        final List<KamisDailyPriceItemResult> items = response.response().body().items().items().stream()
                .map(this::toResult)
                .toList();
        return new KamisDailyPriceResult(header.resultCode(), header.resultMsg(), items);
    }

    private String errorMessage(final DailyPriceResponse.Header header) {
        if (header.resultMsg() == null || header.resultMsg().isBlank()) {
            return DEFAULT_ERROR_MESSAGE;
        }
        return header.resultMsg();
    }

    private String toRegDay(final KamisDailyPriceQuery query) {
        if (query.regDay() == null) {
            return null;
        }
        return query.regDay().toString();
    }

    private KamisDailyPriceItemResult toResult(final Item item) {
        return new KamisDailyPriceItemResult(
                item.corpGdsItemNm(),
                item.corpGdsCd(),
                item.corpGdsVrtyNm(),
                item.gdsSclsfCd(),
                null,
                unit(item),
                item.scsbdDt(),
                item.scsbdPrc(),
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
                null);
    }

    private String unit(final Item item) {
        if (item.unitQty() == null || item.unitQty().isBlank()) {
            return item.unitNm();
        }
        if (item.unitNm() == null || item.unitNm().isBlank()) {
            return item.unitQty();
        }
        return item.unitQty() + item.unitNm();
    }
}
