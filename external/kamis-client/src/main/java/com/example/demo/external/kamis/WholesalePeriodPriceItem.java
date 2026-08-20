package com.example.demo.external.kamis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WholesalePeriodPriceItem(
        @JsonProperty("itemname") String itemName,
        @JsonProperty("kindname") String kindName,
        @JsonProperty("countyname") String countyName,
        @JsonProperty("marketname") String marketName,
        String yyyy,
        @JsonProperty("regday") String regDay,
        String price,
        String unit) {}
