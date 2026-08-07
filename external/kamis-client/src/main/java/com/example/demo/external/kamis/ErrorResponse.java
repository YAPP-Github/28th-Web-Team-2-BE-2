package com.example.demo.external.kamis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ErrorResponse(
        @JsonProperty("OpenAPI_ServiceResponse") OpenApiServiceResponse openApiServiceResponse) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OpenApiServiceResponse(CmmMsgHeader cmmMsgHeader) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CmmMsgHeader(String errMsg, String returnAuthMsg, String returnReasonCode) {}
}
