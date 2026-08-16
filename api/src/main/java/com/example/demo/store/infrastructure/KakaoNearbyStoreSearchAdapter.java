package com.example.demo.store.infrastructure;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import com.example.demo.external.kakao.KakaoCategorySearchResult;
import com.example.demo.external.kakao.KakaoClientException;
import com.example.demo.external.kakao.KakaoLocalClient;
import com.example.demo.external.kakao.KakaoPlace;
import com.example.demo.store.application.port.NearbyStoreSearchPort;
import com.example.demo.store.application.query.NearbyStoreQuery;
import com.example.demo.store.application.result.NearbyStoreResult;
import com.example.demo.store.application.result.NearbyStoreSearchResult;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KakaoNearbyStoreSearchAdapter implements NearbyStoreSearchPort {

    private static final String CATEGORY_GROUP_CODE = "MT1";
    private static final String SORT = "distance";
    private static final int SIZE = 15;

    private final KakaoLocalClient kakaoLocalClient;

    @Override
    public NearbyStoreSearchResult search(final NearbyStoreQuery query) {
        try {
            final KakaoCategorySearchResult result = kakaoLocalClient.searchCategory(
                    CATEGORY_GROUP_CODE,
                    query.longitude(),
                    query.latitude(),
                    query.radius(),
                    SORT,
                    SIZE);
            return toSearchResult(result);
        } catch (final KakaoClientException exception) {
            throw externalApiException(exception);
        }
    }

    private NearbyStoreSearchResult toSearchResult(final KakaoCategorySearchResult result) {
        final List<NearbyStoreResult> stores = result.places().stream()
                .map(this::toStoreResult)
                .toList();
        return new NearbyStoreSearchResult(result.totalCount(), stores);
    }

    private NearbyStoreResult toStoreResult(final KakaoPlace place) {
        return new NearbyStoreResult(
                place.id(),
                place.placeName(),
                place.latitude(),
                place.longitude(),
                place.addressName(),
                place.roadAddressName(),
                place.phone(),
                place.placeUrl(),
                place.distanceMeters(),
                false);
    }

    private ApiException externalApiException(final KakaoClientException exception) {
        return new ApiException(
                ErrorType.EXTERNAL_API_ERROR.description(),
                ErrorType.EXTERNAL_API_ERROR,
                HttpStatus.BAD_GATEWAY,
                exception);
    }
}
