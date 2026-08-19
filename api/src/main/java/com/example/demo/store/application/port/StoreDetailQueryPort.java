package com.example.demo.store.application.port;

import com.example.demo.store.application.result.StoreDetailSnapshot;
import com.example.demo.store.application.result.StoreReportSummary;
import java.time.LocalDate;
import java.util.Optional;

public interface StoreDetailQueryPort {

    Optional<StoreDetailSnapshot> findStore(Long storeId);

    StoreDetailSnapshot saveDetails(StoreDetailSnapshot snapshot);

    boolean isLiked(Long userId, Long storeId);

    long countFavorites(Long storeId);

    StoreReportSummary findReportSummary(Long storeId, LocalDate since);
}
