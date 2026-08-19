package com.example.demo.store.application.usecase;

import com.example.demo.store.application.port.RecommendedStoreQueryPort;
import com.example.demo.store.application.query.RecommendedStoreQuery;
import com.example.demo.store.application.result.RecommendedStoreResult;
import com.example.demo.store.application.result.RecommendedStoreSource;
import com.example.demo.store.application.result.RecommendedStoresResult;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetRecommendedStoresUseCase {

    private static final int CONTENT_LIMIT = 15;
    private static final double EARTH_RADIUS_METERS = 6_371_000;
    private final RecommendedStoreQueryPort recommendedStoreQueryPort;

    public RecommendedStoresResult execute(final RecommendedStoreQuery query) {
        final Map<Long, List<RecommendedStoreSource>> grouped = recommendedStoreQueryPort
                .findLatestCheapReports().stream()
                .filter(source -> distanceMeters(query, source) <= query.radius())
                .collect(java.util.stream.Collectors.groupingBy(
                        RecommendedStoreSource::storeId, LinkedHashMap::new,
                        java.util.stream.Collectors.toList()));
        final List<RecommendedStoreResult> stores = grouped.values().stream()
                .map(sources -> toResult(sources, query))
                .sorted(Comparator.comparing(RecommendedStoreResult::distanceMeters)
                        .thenComparing(RecommendedStoreResult::storeId))
                .toList();
        return new RecommendedStoresResult(stores.size(), stores.stream().limit(CONTENT_LIMIT).toList());
    }

    private RecommendedStoreResult toResult(
            final List<RecommendedStoreSource> sources, final RecommendedStoreQuery query) {
        final RecommendedStoreSource source = sources.getFirst();
        final int count = sources.size();
        final int displayed = Math.min(5, count);
        return new RecommendedStoreResult(
                source.storeId(), source.storeName(), source.latitude(), source.longitude(),
                source.addressName(), source.roadAddressName(), source.phone(), source.placeUrl(),
                distanceMeters(query, source), count,
                sources.stream().limit(displayed).map(RecommendedStoreSource::itemName).toList(),
                count - displayed);
    }

    private int distanceMeters(final RecommendedStoreQuery query, final RecommendedStoreSource source) {
        return distanceMeters(query.latitude(), query.longitude(), source.latitude(), source.longitude());
    }

    private int distanceMeters(
            final BigDecimal originLatitude, final BigDecimal originLongitude,
            final BigDecimal targetLatitude, final BigDecimal targetLongitude) {
        final double latitudeDifference = Math.toRadians(targetLatitude.doubleValue() - originLatitude.doubleValue());
        final double longitudeDifference = Math.toRadians(targetLongitude.doubleValue() - originLongitude.doubleValue());
        final double origin = Math.toRadians(originLatitude.doubleValue());
        final double target = Math.toRadians(targetLatitude.doubleValue());
        final double haversine = Math.sin(latitudeDifference / 2) * Math.sin(latitudeDifference / 2)
                + Math.cos(origin) * Math.cos(target)
                * Math.sin(longitudeDifference / 2) * Math.sin(longitudeDifference / 2);
        return (int) Math.round(EARTH_RADIUS_METERS * 2
                * Math.atan2(Math.sqrt(haversine), Math.sqrt(1 - haversine)));
    }
}
