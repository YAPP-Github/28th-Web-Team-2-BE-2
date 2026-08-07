package com.example.demo.external.kamis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KamisResponse(@JsonProperty("response") KamisResponseData response) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record KamisResponseData(KamisResponseHeader header, KamisResponseBody body) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record KamisResponseHeader(String resultCode, String resultMsg) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record KamisResponseBody(
            KamisResponseItems items,
            String dataType,
            Integer numOfRows,
            Integer pageNo,
            Integer totalCount) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record KamisResponseItems(@JsonProperty("item") List<KamisItem> item) {

        public List<KamisItem> items() {
            if (item == null) {
                return List.of();
            }
            return item;
        }
    }
}
