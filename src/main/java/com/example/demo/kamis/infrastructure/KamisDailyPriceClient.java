package com.example.demo.kamis.infrastructure;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.example.demo.kamis.application.port.KamisPriceQueryPort;
import com.example.demo.kamis.application.query.KamisDailyPriceQuery;
import com.example.demo.kamis.application.result.KamisDailyPriceItemResult;
import com.example.demo.kamis.application.result.KamisDailyPriceResult;
import java.util.List;
import java.util.Objects;
import org.springframework.web.client.RestClient;

final class KamisDailyPriceClient implements KamisPriceQueryPort {

    private final RestClient restClient;
    private final KamisCredentials credentials;

    KamisDailyPriceClient(final RestClient restClient, final KamisCredentials credentials) {
        this.restClient = restClient;
        this.credentials = credentials;
    }

    @Override
    public KamisDailyPriceResult findDailyPrices(final KamisDailyPriceQuery query) {
        validateCredentials();
        final KamisApiResponse response = Objects.requireNonNull(request(query), "KAMIS response is empty");
        final KamisApiData data = Objects.requireNonNull(response.data(), "KAMIS response data is empty");
        return new KamisDailyPriceResult(
                data.errorCode(),
                data.errorMessage(),
                data.items().stream().map(this::toResult).toList());
    }

    private KamisApiResponse request(final KamisDailyPriceQuery query) {
        return restClient.get()
                .uri(uriBuilder -> {
                    uriBuilder
                            .path("/xml.do")
                            .queryParam("action", "dailyPriceByCategoryList")
                            .queryParam("p_product_cls_code", query.productClsCode())
                            .queryParam("p_item_category_code", query.itemCategoryCode())
                            .queryParam("p_cert_key", credentials.certKey())
                            .queryParam("p_cert_id", credentials.certId())
                            .queryParam("p_returntype", "json")
                            .queryParam("p_convert_kg_yn", query.convertKgYn());
                    addOptionalParameter(uriBuilder, "p_country_code", query.countryCode());
                    addOptionalParameter(uriBuilder, "p_regday", query.regDay());
                    return uriBuilder.build();
                })
                .retrieve()
                .body(KamisApiResponse.class);
    }

    private void addOptionalParameter(
            final org.springframework.web.util.UriBuilder uriBuilder,
            final String name,
            final Object value) {
        if (value != null) {
            uriBuilder.queryParam(name, value);
        }
    }

    private KamisDailyPriceItemResult toResult(final KamisApiItem item) {
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

    private void validateCredentials() {
        if (credentials.certKey() == null || credentials.certKey().isBlank()) {
            throw new IllegalStateException("KAMIS credentials are not configured");
        }
        if (credentials.certId() == null || credentials.certId().isBlank()) {
            throw new IllegalStateException("KAMIS credentials are not configured");
        }
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
record KamisApiResponse(KamisApiData data) {}

@JsonIgnoreProperties(ignoreUnknown = true)
record KamisApiData(
        @JsonProperty("error_code") String errorCode,
        @JsonProperty("error_msg")
                @JsonAlias("error_message")
                String errorMessage,
        List<KamisApiItem> item) {

    List<KamisApiItem> items() {
        if (item == null) {
            return List.of();
        }
        return item;
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
record KamisApiItem(
        @JsonProperty("item_name") String itemName,
        @JsonProperty("itemcode") String itemCode,
        @JsonProperty("kind_name") String kindName,
        @JsonProperty("kindcode") String kindCode,
        String rank,
        String unit,
        String day1,
        String dpr1,
        String day2,
        String dpr2,
        String day3,
        String dpr3,
        String day4,
        String dpr4,
        String day5,
        String dpr5,
        String day6,
        String dpr6,
        String day7,
        String dpr7) {}
