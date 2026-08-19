package com.example.demo.mypage.application.usecase;

import com.example.demo.mypage.application.port.FavoriteStoreQueryPort;
import com.example.demo.mypage.application.query.FavoriteStoresQuery;
import com.example.demo.mypage.application.result.FavoriteStoreResult;
import com.example.demo.mypage.application.result.FavoriteStoreSource;
import com.example.demo.mypage.application.result.FavoriteStoresResult;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetFavoriteStoresUseCase {

    private static final String UNKNOWN_OPEN_STATUS = "UNKNOWN";
    private final FavoriteStoreQueryPort favoriteStoreQueryPort;

    @Transactional(readOnly = true)
    public FavoriteStoresResult execute(final FavoriteStoresQuery query) {
        final List<FavoriteStoreSource> sources = favoriteStoreQueryPort.findAll(query).stream()
                .sorted(order(query))
                .toList();
        final List<FavoriteStoreResult> stores = pageContent(sources, query).stream()
                .map(source -> toResult(source, query))
                .toList();
        return new FavoriteStoresResult(
                stores,
                query.page(),
                query.size(),
                sources.size(),
                totalPages(sources.size(), query.size()),
                ((long) query.page() + 1) * query.size() < sources.size());
    }

    private Comparator<FavoriteStoreSource> order(final FavoriteStoresQuery query) {
        if (!query.hasCoordinates()) {
            return Comparator.comparing(FavoriteStoreSource::storeId);
        }
        final Comparator<Integer> distanceComparator = Comparator.nullsLast(Comparator.naturalOrder());
        return Comparator.comparing(
                        (FavoriteStoreSource source) -> distanceMeters(query, source),
                        distanceComparator)
                .thenComparing(FavoriteStoreSource::storeId);
    }

    private List<FavoriteStoreSource> pageContent(
            final List<FavoriteStoreSource> sources, final FavoriteStoresQuery query) {
        final long offset = (long) query.page() * query.size();
        if (offset >= sources.size()) {
            return List.of();
        }
        final int fromIndex = Math.toIntExact(offset);
        final int toIndex = Math.min(fromIndex + query.size(), sources.size());
        return sources.subList(fromIndex, toIndex);
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

    private int totalPages(final long totalElements, final int size) {
        if (totalElements == 0) {
            return 0;
        }
        return Math.toIntExact((totalElements + size - 1) / size);
    }
}
