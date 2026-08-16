package com.example.demo.external.kakao;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

public class KakaoLocalRestClient implements KakaoLocalClient {

    private static final String CATEGORY_CODE = "MT1";
    private static final String SORT = "distance";
    private static final int SIZE = 15;
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(5);

    private final RestClient restClient;
    private final String apiKey;

    public KakaoLocalRestClient(final String baseUrl, final String apiKey) {
        this(RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory()), apiKey);
    }

    KakaoLocalRestClient(final RestClient.Builder builder, final String apiKey) {
        this.restClient = builder.build();
        this.apiKey = apiKey;
    }

    @Override
    public KakaoCategorySearchResult searchCategory(final KakaoCategorySearchQuery query) {
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
            return toResult(response);
        } catch (final RuntimeException exception) {
            throw new KakaoClientException(exception);
        }
    }

    private KakaoCategorySearchResult toResult(final KakaoResponse response) {
        if (response == null || response.documents() == null || response.meta() == null) {
            throw new IllegalStateException("Invalid Kakao response");
        }
        final List<KakaoPlace> places = response.documents().stream()
                .map(document -> new KakaoPlace(
                        document.id(),
                        document.placeName(),
                        new BigDecimal(document.y()),
                        new BigDecimal(document.x()),
                        document.addressName(),
                        document.roadAddressName(),
                        document.phone(),
                        document.placeUrl(),
                        distanceMeters(document.distance())))
                .toList();
        return new KakaoCategorySearchResult(response.meta().totalCount(), places);
    }

    private Integer distanceMeters(final String distance) {
        if (distance == null || distance.isBlank()) {
            return null;
        }
        return Integer.valueOf(distance);
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
