package com.example.demo.external.kamis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Builder;

@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public record DailyPriceResponse(
        @JsonProperty("error_code") String errorCode,
        @JsonProperty("item") List<Item> items,
        Meta meta) {

    public DailyPriceResponse {
        if (items == null) {
            items = List.of();
        }
    }
}
