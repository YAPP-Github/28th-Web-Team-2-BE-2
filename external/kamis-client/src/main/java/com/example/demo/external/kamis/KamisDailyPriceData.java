package com.example.demo.external.kamis;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KamisDailyPriceData(
        @JsonProperty("error_code") String errorCode,
        @JsonProperty("error_msg") @JsonAlias("error_message") String errorMessage,
        @JsonProperty("item") List<KamisDailyPriceItem> item) {

    public List<KamisDailyPriceItem> items() {
        if (item == null) {
            return List.of();
        }
        return item;
    }
}
