package com.example.demo.store.application.usecase;

import com.example.demo.store.application.port.RecommendedStoreQueryPort;
import com.example.demo.store.application.query.RecommendedStoreQuery;
import com.example.demo.store.application.result.RecommendedStoreResult;
import com.example.demo.store.application.result.RecommendedStoresResult;
import com.example.demo.store.application.result.RecommendedStoreSource;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetRecommendedStoresUseCase {

    private static final int CONTENT_LIMIT = 15;
    private static final double EARTH_RADIUS_METERS = 6_371_000;

    private final RecommendedStoreQueryPort recommendedStoreQueryPort;

    public RecommendedStoresResult execute(final RecommendedStoreQuery query) {
        final List<RecommendedStoreResult> stores = recommendedStoreQueryPort
                .findLatestCheapReports(query.itemId()).stream()
                .map(source -> toResult(source, query))
                .filter(store -> store.distanceMeters() <= query.radius())
                .sorted(Comparator.comparing(RecommendedStoreResult::distanceMeters)
                        .thenComparing(RecommendedStoreResult::storeId))
                .toList();
        return new RecommendedStoresResult(
                stores.size(), stores.stream().limit(CONTENT_LIMIT).toList());
    }

    private RecommendedStoreResult toResult(
            final RecommendedStoreSource source,
            final RecommendedStoreQuery query) {
        return new RecommendedStoreResult(
                source.storeId(),
                source.storeName(),
                source.latitude(),
                source.longitude(),
                source.addressName(),
                source.roadAddressName(),
                source.phone(),
                source.placeUrl(),
                distanceMeters(query.latitude(), query.longitude(), source.latitude(), source.longitude()),
                source.price(),
                source.reportedDate(),
                source.priceDiffRate());
    }

    private int distanceMeters(
            final BigDecimal originLatitude,
            final BigDecimal originLongitude,
            final BigDecimal targetLatitude,
            final BigDecimal targetLongitude) {
        final double latitudeDifference = Math.toRadians(
                targetLatitude.doubleValue() - originLatitude.doubleValue());
        final double longitudeDifference = Math.toRadians(
                targetLongitude.doubleValue() - originLongitude.doubleValue());
        final double originLatitudeRadians = Math.toRadians(originLatitude.doubleValue());
        final double targetLatitudeRadians = Math.toRadians(targetLatitude.doubleValue());
        final double haversine = Math.sin(latitudeDifference / 2) * Math.sin(latitudeDifference / 2)
                + Math.cos(originLatitudeRadians) * Math.cos(targetLatitudeRadians)
                * Math.sin(longitudeDifference / 2) * Math.sin(longitudeDifference / 2);
        return (int) Math.round(EARTH_RADIUS_METERS * 2 * Math.atan2(
                Math.sqrt(haversine), Math.sqrt(1 - haversine)));
    }
}
