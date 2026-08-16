package com.example.demo.store.infrastructure;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import com.example.demo.store.application.port.NearbyStoreSearchPort;
import com.example.demo.store.application.query.NearbyStoreQuery;
import com.example.demo.store.application.result.NearbyStoreResult;
import com.example.demo.store.application.result.NearbyStoreSearchResult;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class KakaoNearbyStoreSearchAdapter implements NearbyStoreSearchPort {

    private static final String CATEGORY_CODE = "MT1";
    private static final String SORT = "distance";
    private static final int SIZE = 15;
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(5);

    private final RestClient restClient;
    private final String apiKey;

    @Autowired
    public KakaoNearbyStoreSearchAdapter(@Value("${kakao.local.base-url:https://dapi.kakao.com}") final String baseUrl,
            @Value("${kakao.local.rest-api-key:}") final String apiKey) {
        this(RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory()), apiKey);
    }

    KakaoNearbyStoreSearchAdapter(final RestClient.Builder builder, final String apiKey) {
        this.restClient = builder.build();
        this.apiKey = apiKey;
    }

    @Override
    public NearbyStoreSearchResult search(final NearbyStoreQuery query) {
        try {
            final KakaoResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v2/local/search/category.json")
                            .queryParam("category_group_code", CATEGORY_CODE)
                            .queryParam("x", query.longitude())
                            .queryParam("y", query.latitude())
                            .queryParam("radius", query.radius())
                            .queryParam("sort", SORT)
                            .queryParam("size", SIZE)
                            .build())
                    .header("Authorization", "KakaoAK " + apiKey)
                    .retrieve()
                    .body(KakaoResponse.class);
            return toSearchResult(response);
        } catch (final RuntimeException exception) {
            throw externalApiException();
        }
    }

    private NearbyStoreSearchResult toSearchResult(final KakaoResponse response) {
        if (response == null || response.documents() == null || response.meta() == null) {
            throw externalApiException();
        }
        final List<NearbyStoreResult> stores = response.documents().stream()
                .map(document -> new NearbyStoreResult(
                        document.id(),
                        document.placeName(),
                        new BigDecimal(document.y()),
                        new BigDecimal(document.x()),
                        document.addressName(),
                        document.roadAddressName(),
                        document.phone(),
                        document.placeUrl(),
                        distanceMeters(document.distance()),
                        false))
                .toList();
        return new NearbyStoreSearchResult(response.meta().totalCount(), stores);
    }

    private Integer distanceMeters(final String distance) {
        if (distance == null || distance.isBlank()) {
            return null;
        }
        return Integer.valueOf(distance);
    }

    private ApiException externalApiException() {
        return new ApiException(
                ErrorType.EXTERNAL_API_ERROR.description(),
                ErrorType.EXTERNAL_API_ERROR,
                HttpStatus.BAD_GATEWAY);
    }

    private static ClientHttpRequestFactory requestFactory() {
        final SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT);
        factory.setReadTimeout(READ_TIMEOUT);
        return factory;
    }

    private record KakaoResponse(KakaoMeta meta, List<KakaoDocument> documents) {}

    private record KakaoMeta(@JsonProperty("total_count") long totalCount) {}

    private record KakaoDocument(
            String id,
            @JsonProperty("place_name") String placeName,
            String x,
            String y,
            @JsonProperty("address_name") String addressName,
            @JsonProperty("road_address_name") String roadAddressName,
            String phone,
            @JsonProperty("place_url") String placeUrl,
            String distance) {}
}
