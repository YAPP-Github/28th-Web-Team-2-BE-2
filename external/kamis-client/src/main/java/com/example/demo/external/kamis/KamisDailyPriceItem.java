package com.example.demo.external.kamis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KamisDailyPriceItem(
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
