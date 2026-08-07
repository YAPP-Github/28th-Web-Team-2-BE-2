package com.example.demo.external.kamis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Meta(
        @JsonProperty("dataType") String dataType,
        @JsonProperty("numOfRows") Integer numOfRows,
        @JsonProperty("pageNo") Integer pageNo,
        @JsonProperty("totalCount") Integer totalCount) {}
