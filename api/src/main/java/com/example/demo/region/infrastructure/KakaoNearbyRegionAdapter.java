package com.example.demo.region.infrastructure;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import com.example.demo.region.application.port.NearbyRegionQueryPort;
import com.example.demo.region.application.query.NearbyRegionQuery;
import com.example.demo.region.application.result.NearbyRegionResult;
import com.fasterxml.jackson.annotation.JsonProperty;
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
public class KakaoNearbyRegionAdapter implements NearbyRegionQueryPort {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(5);

    private final RestClient restClient;
    private final String apiKey;

    @Autowired
    public KakaoNearbyRegionAdapter(
            @Value("${kakao.local.base-url:https://dapi.kakao.com}") final String baseUrl,
            @Value("${kakao.local.rest-api-key:}") final String apiKey) {
        this(RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory()), apiKey);
    }

    KakaoNearbyRegionAdapter(final RestClient.Builder builder, final String apiKey) {
        this.restClient = builder.build();
        this.apiKey = apiKey;
    }

    @Override
    public NearbyRegionResult find(final NearbyRegionQuery query) {
        try {
            final KakaoResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v2/local/geo/coord2regioncode.json")
                            .queryParam("x", query.longitude())
                            .queryParam("y", query.latitude())
                            .build())
                    .header("Authorization", "KakaoAK " + apiKey)
                    .retrieve()
                    .body(KakaoResponse.class);
            return toResult(response);
        } catch (final RuntimeException exception) {
            throw externalApiException();
        }
    }

    private NearbyRegionResult toResult(final KakaoResponse response) {
        if (response == null || response.documents() == null || response.meta() == null) {
            throw externalApiException();
        }
        final List<NearbyRegionResult.Region> regions = response.documents().stream()
                .filter(document -> "B".equals(document.regionType()))
                .map(this::toRegion)
                .toList();
        return new NearbyRegionResult(regions);
    }

    private NearbyRegionResult.Region toRegion(final KakaoDocument document) {
        if (document.code() == null || document.code().isBlank()) {
            throw externalApiException();
        }
        return new NearbyRegionResult.Region(
                document.code(), regionName(document.region2DepthName(), document.region3DepthName()));
    }

    private String regionName(final String region2DepthName, final String region3DepthName) {
        if (region2DepthName == null || region3DepthName == null
                || region2DepthName.isBlank() || region3DepthName.isBlank()) {
            throw externalApiException();
        }
        return region2DepthName + " " + region3DepthName;
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

    private record KakaoMeta(@JsonProperty("total_count") int totalCount) {}

    private record KakaoDocument(
            @JsonProperty("region_type") String regionType,
            String code,
            @JsonProperty("region_2depth_name") String region2DepthName,
            @JsonProperty("region_3depth_name") String region3DepthName) {}
}
