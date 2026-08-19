package com.example.demo.mypage.application.usecase;

import com.example.demo.mypage.application.port.FavoriteStoreQueryPort;
import com.example.demo.mypage.application.query.FavoriteStoresQuery;
import com.example.demo.mypage.application.result.FavoriteStoreResult;
import com.example.demo.mypage.application.result.FavoriteStoreSource;
import com.example.demo.mypage.application.result.FavoriteStoresResult;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetFavoriteStoresUseCase {

    private static final String UNKNOWN_OPEN_STATUS = "UNKNOWN";
    private final FavoriteStoreQueryPort favoriteStoreQueryPort;

    @Transactional(readOnly = true)
    public FavoriteStoresResult execute(final FavoriteStoresQuery query) {
        final Page<FavoriteStoreSource> page = favoriteStoreQueryPort.findAll(query);
        final List<FavoriteStoreResult> stores = page.getContent().stream()
                .map(source -> toResult(source, query))
                .toList();
        return new FavoriteStoresResult(
                stores,
                query.page(),
                query.size(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext());
    }

    private FavoriteStoreResult toResult(
            final FavoriteStoreSource source, final FavoriteStoresQuery query) {
        return new FavoriteStoreResult(
                source.storeId(),
                source.storeName(),
                null,
                distanceMeters(query, source),
                UNKNOWN_OPEN_STATUS,
                null,
                true);
    }

    private Integer distanceMeters(
            final FavoriteStoresQuery query, final FavoriteStoreSource source) {
        if (!query.hasCoordinates() || source.latitude() == null || source.longitude() == null) {
            return null;
        }
        final double latitudeDistance = Math.toRadians(
                source.latitude().doubleValue() - query.latitude().doubleValue());
        final double longitudeDistance = Math.toRadians(
                source.longitude().doubleValue() - query.longitude().doubleValue());
        final double sourceLatitude = Math.toRadians(source.latitude().doubleValue());
        final double queryLatitude = Math.toRadians(query.latitude().doubleValue());
        final double haversine = Math.sin(latitudeDistance / 2) * Math.sin(latitudeDistance / 2)
                + Math.cos(queryLatitude)
                        * Math.cos(sourceLatitude)
                        * Math.sin(longitudeDistance / 2)
                        * Math.sin(longitudeDistance / 2);
        return Math.toIntExact(Math.round(6_371_000 * 2 * Math.atan2(Math.sqrt(haversine), Math.sqrt(1 - haversine))));
    }

}
