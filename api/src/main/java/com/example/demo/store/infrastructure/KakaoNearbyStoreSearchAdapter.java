package com.example.demo.store.infrastructure;

import com.example.demo.external.kakao.KakaoCategorySearchResult;
import com.example.demo.external.kakao.KakaoPlace;
import com.example.demo.external.kakao.feign.KakaoMapClient;
import com.example.demo.store.application.port.NearbyStoreSearchPort;
import com.example.demo.store.application.query.NearbyStoreQuery;
import com.example.demo.store.application.result.NearbyStoreResult;
import com.example.demo.store.application.result.NearbyStoreSearchResult;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class KakaoNearbyStoreSearchAdapter implements NearbyStoreSearchPort {

    private static final String CATEGORY_GROUP_CODE = "MT1";
    private static final String SORT = "distance";
    private static final int SIZE = 15;

    private final KakaoMapClient kakaoMapClient;

    @Override
    public NearbyStoreSearchResult search(final NearbyStoreQuery query) {
        log.info(
                "[KakaoStoreSearch] request longitude={} latitude={} radius={} category={} sort={} size={}",
                query.longitude(),
                query.latitude(),
                query.radius(),
                CATEGORY_GROUP_CODE,
                SORT,
                SIZE);
        try {
            final KakaoCategorySearchResult result = kakaoMapClient.searchCategory(
                    CATEGORY_GROUP_CODE,
                    query.longitude(),
                    query.latitude(),
                    query.radius(),
                    SORT,
                    SIZE);
            log.info(
                    "[KakaoStoreSearch] response totalCount={} placeCount={}",
                    result.totalCount(),
                    result.places().size());
            final NearbyStoreSearchResult searchResult = toSearchResult(result);
            log.info(
                    "[KakaoStoreSearch] mapped totalCount={} storeCount={}",
                    searchResult.totalCount(),
                    searchResult.stores().size());
            return searchResult;
        } catch (final RuntimeException exception) {
            log.error(
                    "[KakaoStoreSearch] failed longitude={} latitude={} radius={} exceptionType={} message={}",
                    query.longitude(),
                    query.latitude(),
                    query.radius(),
                    exception.getClass().getSimpleName(),
                    exception.getMessage(),
                    exception);
            throw exception;
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
}
