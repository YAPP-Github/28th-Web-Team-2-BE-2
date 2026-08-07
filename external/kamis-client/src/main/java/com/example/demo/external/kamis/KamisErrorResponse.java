package com.example.demo.external.kamis;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KamisErrorResponse(KamisErrorData data) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record KamisErrorData(
            @JsonProperty("error_code") String errorCode,
            @JsonProperty("error_msg") @JsonAlias("error_message") String errorMessage) {}
}
