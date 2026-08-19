package com.example.demo.store.application.usecase;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import com.example.demo.store.application.port.StoreDetailQueryPort;
import com.example.demo.store.application.query.StoreDetailQuery;
import com.example.demo.store.application.result.StoreDetailResult;
import com.example.demo.store.application.result.StoreDetailSnapshot;
import com.example.demo.store.application.result.StoreReportSummary;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetStoreDetailUseCase {

    private static final int RECENT_REPORT_DAYS = 30;
    private static final double EARTH_RADIUS_METERS = 6_371_000;

    private final StoreDetailQueryPort storeDetailQueryPort;

    @Transactional(readOnly = true)
    public StoreDetailResult execute(final StoreDetailQuery query) {
        final StoreDetailSnapshot store = storeDetailQueryPort.findStore(query.storeId())
                .orElseThrow(this::storeNotFound);
        final StoreReportSummary reports = storeDetailQueryPort.findReportSummary(
                query.storeId(), LocalDate.now().minusDays(RECENT_REPORT_DAYS - 1L));
        final boolean isLiked = query.userId() != null
                && storeDetailQueryPort.isLiked(query.userId(), query.storeId());

        return new StoreDetailResult(
                store.storeId(),
                store.storeName(),
                null,
                isLiked,
                storeDetailQueryPort.countFavorites(store.storeId()),
                reports.cheapItemCount(),
                reports.expensiveItemCount(),
                reports.totalReportedItemCount(),
                null,
                null,
                reports.latestReportedDate(),
                reports.latestReportedAt(),
                store.address(),
                store.latitude(),
                store.longitude(),
                distanceMeters(store, query.latitude(), query.longitude()),
                null,
                null,
                "UNKNOWN");
    }

    private ApiException storeNotFound() {
        return new ApiException(
                ErrorType.NO_RESOURCE_ERROR.description(),
                ErrorType.NO_RESOURCE_ERROR,
                HttpStatus.NOT_FOUND);
    }

    private Integer distanceMeters(
            final StoreDetailSnapshot store,
            final BigDecimal latitude,
            final BigDecimal longitude) {
        if (latitude == null || longitude == null || store.latitude() == null || store.longitude() == null) {
            return null;
        }
        final double latitudeDelta = Math.toRadians(latitude.doubleValue() - store.latitude().doubleValue());
        final double longitudeDelta = Math.toRadians(longitude.doubleValue() - store.longitude().doubleValue());
        final double storeLatitude = Math.toRadians(store.latitude().doubleValue());
        final double requestedLatitude = Math.toRadians(latitude.doubleValue());
        final double haversine = Math.sin(latitudeDelta / 2) * Math.sin(latitudeDelta / 2)
                + Math.cos(storeLatitude) * Math.cos(requestedLatitude)
                        * Math.sin(longitudeDelta / 2) * Math.sin(longitudeDelta / 2);
        final double arc = 2 * Math.atan2(Math.sqrt(haversine), Math.sqrt(1 - haversine));
        return (int) Math.round(EARTH_RADIUS_METERS * arc);
    }
}
