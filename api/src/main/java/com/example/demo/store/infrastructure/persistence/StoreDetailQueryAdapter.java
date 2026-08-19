package com.example.demo.store.infrastructure.persistence;

import com.example.demo.report.domain.Store;
import com.example.demo.store.application.port.StoreDetailQueryPort;
import com.example.demo.store.application.result.StoreDetailSnapshot;
import com.example.demo.store.application.result.StoreReportSummary;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StoreDetailQueryAdapter implements StoreDetailQueryPort {

    private static final String REPORT_SUMMARY = """
            SELECT COUNT(*) AS total_reported_item_count,
                   COALESCE(SUM(CASE WHEN public_price_diff < 0 THEN 1 ELSE 0 END), 0)
                       AS cheap_item_count,
                   COALESCE(SUM(CASE WHEN public_price_diff > 0 THEN 1 ELSE 0 END), 0)
                       AS expensive_item_count,
                   MAX(report_date) AS latest_reported_date,
                   MAX(created_at) AS latest_reported_at
              FROM user_reports
             WHERE store_id = :storeId
               AND report_date >= :since
            """;

    private final StoreJpaRepository storeJpaRepository;
    private final StoreFavoriteJpaRepository storeFavoriteJpaRepository;
    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public Optional<StoreDetailSnapshot> findStore(final Long storeId) {
        return storeJpaRepository.findById(storeId).map(this::toSnapshot);
    }

    @Override
    public StoreDetailSnapshot saveDetails(final StoreDetailSnapshot snapshot) {
        final Store store = storeJpaRepository.findById(snapshot.storeId()).orElseThrow();
        store.updateKakaoDetails(
                snapshot.storeImageUrl(),
                snapshot.businessHours() == null ? null : String.join("\n", snapshot.businessHours()),
                snapshot.openStatus(),
                Instant.now());
        return toSnapshot(storeJpaRepository.save(store));
    }

    @Override
    public boolean isLiked(final Long userId, final Long storeId) {
        return storeFavoriteJpaRepository.existsByUserIdAndStoreId(userId, storeId);
    }

    @Override
    public long countFavorites(final Long storeId) {
        return storeFavoriteJpaRepository.countByStoreId(storeId);
    }

    @Override
    public StoreReportSummary findReportSummary(final Long storeId, final LocalDate since) {
        return jdbcTemplate.queryForObject(
                REPORT_SUMMARY,
                Map.of("storeId", storeId, "since", since),
                this::toReportSummary);
    }

    private StoreDetailSnapshot toSnapshot(final Store store) {
        return new StoreDetailSnapshot(
                store.id(),
                store.placeName(),
                store.addressName(),
                store.latitude(),
                store.longitude(),
                store.placeUrl(),
                store.imageUrl(),
                splitHours(store.businessHours()),
                store.openStatus() == null ? "UNKNOWN" : store.openStatus(),
                store.kakaoDetailsCollectedAt());
    }

    private java.util.List<String> splitHours(final String businessHours) {
        if (businessHours == null || businessHours.isBlank()) {
            return null;
        }
        return java.util.List.of(businessHours.split("\\n"));
    }

    private StoreReportSummary toReportSummary(final ResultSet resultSet, final int rowNumber)
            throws SQLException {
        return new StoreReportSummary(
                resultSet.getLong("cheap_item_count"),
                resultSet.getLong("expensive_item_count"),
                resultSet.getLong("total_reported_item_count"),
                toLocalDate(resultSet),
                toInstant(resultSet));
    }

    private LocalDate toLocalDate(final ResultSet resultSet) throws SQLException {
        final java.sql.Date date = resultSet.getDate("latest_reported_date");
        return date == null ? null : date.toLocalDate();
    }

    private Instant toInstant(final ResultSet resultSet) throws SQLException {
        final java.sql.Timestamp timestamp = resultSet.getTimestamp("latest_reported_at");
        return timestamp == null ? null : timestamp.toInstant();
    }
}
