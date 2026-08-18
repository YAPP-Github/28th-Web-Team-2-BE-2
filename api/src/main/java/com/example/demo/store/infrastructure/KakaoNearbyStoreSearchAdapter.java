package com.example.demo.store.infrastructure;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import com.example.demo.external.kakao.KakaoCategorySearchResult;
import com.example.demo.external.kakao.KakaoPlace;
import com.example.demo.external.kakao.feign.KakaoMapClient;
import com.example.demo.store.application.port.NearbyStoreSearchPort;
import com.example.demo.store.application.query.NearbyStoreQuery;
import com.example.demo.store.application.result.NearbyStoreCandidate;
import com.example.demo.store.application.result.NearbyStoreSearchResult;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KakaoNearbyStoreSearchAdapter implements NearbyStoreSearchPort {

    private static final String CATEGORY_GROUP_CODE = "MT1";
    private static final String SORT = "distance";
    private static final int SIZE = 15;
    private static final int MAX_PAGE = 45;
    private static final int MAX_PAGEABLE_COUNT = 45;

    private final KakaoMapClient kakaoMapClient;

    @Override
    public NearbyStoreSearchResult search(final NearbyStoreQuery query) {
        final Map<String, NearbyStoreCandidate> candidates = new LinkedHashMap<>();
        long providerTotalCount = -1;
        long providerPageableCount = -1;
        int fetchedCount = 0;
        for (int page = 1; page <= MAX_PAGE; page++) {
            final KakaoCategorySearchResult result = searchPage(query, page);
            if (result == null || result.places() == null
                    || result.totalCount() < 0
                    || result.pageableCount() < 0
                    || result.totalCount() > result.pageableCount()
                    || result.pageableCount() > MAX_PAGEABLE_COUNT
                    || result.places().size() > SIZE) {
                throw externalApiException();
            }
            if (providerTotalCount == -1) {
                providerTotalCount = result.totalCount();
                providerPageableCount = result.pageableCount();
            }
            if (providerTotalCount != result.totalCount()
                    || providerPageableCount != result.pageableCount()) {
                throw externalApiException();
            }
            fetchedCount += result.places().size();
            for (final KakaoPlace place : result.places()) {
                if (candidates.containsKey(place.id())) {
                    throw externalApiException();
                }
                candidates.put(place.id(), toCandidate(place));
            }
            if (result.end()) {
                if (fetchedCount != providerPageableCount) {
                    throw externalApiException();
                }
                return new NearbyStoreSearchResult(List.copyOf(candidates.values()));
            }
        }
        throw externalApiException();
    }

    private KakaoCategorySearchResult searchPage(final NearbyStoreQuery query, final int page) {
        try {
            return kakaoMapClient.searchCategory(
                    CATEGORY_GROUP_CODE,
                    query.longitude(),
                    query.latitude(),
                    query.radius(),
                    SORT,
                    page,
                    SIZE);
        } catch (final ApiException exception) {
            throw exception;
        } catch (final RuntimeException exception) {
            throw externalApiException();
        }
    }

    private NearbyStoreCandidate toCandidate(final KakaoPlace place) {
        return new NearbyStoreCandidate(
                place.id(),
                place.placeName(),
                place.latitude(),
                place.longitude(),
                place.addressName(),
                place.roadAddressName(),
                place.phone(),
                place.placeUrl(),
                place.distanceMeters());
    }

    private ApiException externalApiException() {
        return new ApiException(
                ErrorType.EXTERNAL_API_ERROR.description(),
                ErrorType.EXTERNAL_API_ERROR,
                HttpStatus.BAD_GATEWAY);
    }
}
