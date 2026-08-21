package com.example.demo.external.kamis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WholesalePeriodPriceResponse(
        @JsonProperty("error_code") String errorCode,
        @JsonProperty("item") List<WholesalePeriodPriceItem> items,
        Meta meta) {

    public WholesalePeriodPriceResponse {
        if (items == null) {
            items = List.of();
        }
    }
}
