package com.example.demo.external.kamis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DailyPriceResponse(Response response) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Response(Header header, Body body) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Header(String resultCode, String resultMsg) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Body(Items items, @JsonUnwrapped Meta meta) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Items(@JsonProperty("item") List<Item> item) {

        public List<Item> items() {
            if (item == null) {
                return List.of();
            }
            return item;
        }
    }
}
