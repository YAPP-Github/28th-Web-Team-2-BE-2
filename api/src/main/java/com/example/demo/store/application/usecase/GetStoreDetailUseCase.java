package com.example.demo.store.application.usecase;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import com.example.demo.image.application.command.UploadImageCommand;
import com.example.demo.image.application.result.UploadedImageResult;
import com.example.demo.image.application.usecase.UploadImageUseCase;
import com.example.demo.image.domain.ImageContentType;
import com.example.demo.image.domain.ImageKey;
import com.example.demo.image.domain.ImageSize;
import com.example.demo.store.application.port.StoreDetailPersistencePort;
import com.example.demo.store.application.port.StoreDetailQueryPort;
import com.example.demo.store.application.port.StorePageSource;
import com.example.demo.store.application.query.StoreDetailQuery;
import com.example.demo.store.application.result.StoreDetailEnrichment;
import com.example.demo.store.application.result.StoreDetailResult;
import com.example.demo.store.application.result.StoreDetailSnapshot;
import com.example.demo.store.application.result.StorePageContent;
import com.example.demo.store.application.result.StoreReportSummary;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class GetStoreDetailUseCase {

    private static final int RECENT_REPORT_DAYS = 30;
    private static final double EARTH_RADIUS_METERS = 6_371_000;
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private final StoreDetailQueryPort storeDetailQueryPort;
    private final StorePageSource storePageSource;
    private final StoreDetailPersistencePort storeDetailPersistencePort;
    private final UploadImageUseCase uploadImageUseCase;

    public StoreDetailResult execute(final StoreDetailQuery query) {
        final StoreDetailSnapshot store = storeDetailQueryPort.findStore(query.storeId())
                .orElseThrow(this::storeNotFound);
        final StoreDetailSnapshot enrichedStore = enrichStore(store);
        final StoreReportSummary reports = storeDetailQueryPort.findReportSummary(
                query.storeId(), LocalDate.now(SEOUL).minusDays(RECENT_REPORT_DAYS - 1L));
        final boolean isLiked = query.userId() != null
                && storeDetailQueryPort.isLiked(query.userId(), query.storeId());

        return new StoreDetailResult(
                enrichedStore.storeId(),
                enrichedStore.storeName(),
                enrichedStore.storeImageUrl(),
                isLiked,
                storeDetailQueryPort.countFavorites(enrichedStore.storeId()),
                reports.cheapItemCount(),
                reports.expensiveItemCount(),
                reports.totalReportedItemCount(),
                enrichedStore.regionId(),
                enrichedStore.regionName(),
                reports.latestReportedDate(),
                reports.latestReportedAt(),
                enrichedStore.address(),
                enrichedStore.latitude(),
                enrichedStore.longitude(),
                distanceMeters(enrichedStore, query.latitude(), query.longitude()),
                null,
                enrichedStore.businessHours(),
                enrichedStore.openStatus());
    }

    private StoreDetailSnapshot enrichStore(final StoreDetailSnapshot store) {
        if (store.placeUrl() == null) {
            return store;
        }
        final StorePageContent page = findPage(store);
        if (page == null) {
            return store;
        }
        final String uploadedImageUrl = store.storeImageUrl() == null
                ? uploadImage(store.storeId(), page)
                : null;
        final List<String> businessHours = page.businessHours().isEmpty()
                ? store.businessHours()
                : page.businessHours();
        final String openStatus = isKnownStatus(page.openStatus())
                ? page.openStatus()
                : store.openStatus();
        final StoreDetailEnrichment enrichment = new StoreDetailEnrichment(
                uploadedImageUrl,
                page.businessHours().isEmpty() ? null : page.businessHours(),
                isKnownStatus(page.openStatus()) ? page.openStatus() : null);
        if (enrichment.hasValues()) {
            storeDetailPersistencePort.update(store.storeId(), enrichment);
        }
        return new StoreDetailSnapshot(
                store.storeId(),
                store.storeName(),
                store.address(),
                store.regionId(),
                store.regionName(),
                store.latitude(),
                store.longitude(),
                store.placeUrl(),
                store.storeImageUrl() == null ? uploadedImageUrl : store.storeImageUrl(),
                businessHours,
                openStatus);
    }

    private StorePageContent findPage(final StoreDetailSnapshot store) {
        try {
            final StorePageContent page = storePageSource.find(store.placeUrl());
            return page == null ? StorePageContent.empty() : page;
        } catch (final RuntimeException exception) {
            log.warn("Kakao store page collection failed for storeId={}", store.storeId(), exception);
            return null;
        }
    }

    private String uploadImage(final Long storeId, final StorePageContent page) {
        if (page.imageContent() == null || page.imageContentType() == null) {
            return null;
        }
        try {
            final ImageContentType contentType = ImageContentType.from(
                    page.imageContentType(), page.imageContent());
            final ImageSize size = new ImageSize(page.imageContent().length);
            final UploadedImageResult result = uploadImageUseCase.execute(
                    ImageKey.forStore(storeId, contentType),
                    new UploadImageCommand(contentType, size, page.imageContent()));
            return result.imageUrl();
        } catch (final RuntimeException exception) {
            log.warn("Kakao store image persistence failed", exception);
            return null;
        }
    }

    private boolean isKnownStatus(final String openStatus) {
        return "OPEN".equals(openStatus) || "CLOSED".equals(openStatus);
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
