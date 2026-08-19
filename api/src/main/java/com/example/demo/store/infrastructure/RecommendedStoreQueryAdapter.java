package com.example.demo.store.infrastructure;

import com.example.demo.item.domain.Item;
import com.example.demo.item.infrastructure.ItemJpaRepository;
import com.example.demo.report.domain.Store;
import com.example.demo.report.domain.UserReport;
import com.example.demo.report.infrastructure.UserReportJpaRepository;
import com.example.demo.store.application.port.RecommendedStoreQueryPort;
import com.example.demo.store.application.result.RecommendedStoreSource;
import com.example.demo.store.infrastructure.persistence.StoreJpaRepository;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RecommendedStoreQueryAdapter implements RecommendedStoreQueryPort {

    private final UserReportJpaRepository userReportJpaRepository;
    private final StoreJpaRepository storeJpaRepository;
    private final ItemJpaRepository itemJpaRepository;

    @Override
    public List<RecommendedStoreSource> findLatestCheapReports() {
        final List<UserReport> reports = userReportJpaRepository.findLatestCheapReports();
        return toSources(reports);
    }

    public List<RecommendedStoreSource> findLatestCheapReports(final String regionId) {
        return toSources(userReportJpaRepository.findLatestCheapReports(regionId));
    }

    private List<RecommendedStoreSource> toSources(final List<UserReport> reports) {
        final Map<Long, Store> stores = storeJpaRepository.findAllById(reports.stream().map(UserReport::storeId).toList())
                .stream().collect(Collectors.toMap(Store::id, Function.identity()));
        final Map<Long, Item> items = itemJpaRepository.findAllById(reports.stream().map(UserReport::itemId).toList())
                .stream().collect(Collectors.toMap(Item::id, Function.identity()));
        return reports.stream().map(report -> toSource(report, stores.get(report.storeId()), items.get(report.itemId())))
                .filter(source -> source != null).toList();
    }

    private RecommendedStoreSource toSource(final UserReport report, final Store store, final Item item) {
        if (store == null || item == null || store.latitude() == null || store.longitude() == null) {
            return null;
        }
        return new RecommendedStoreSource(store.id(), store.placeName(), store.latitude(), store.longitude(),
                store.addressName(), store.roadAddressName(), store.phone(), store.placeUrl(), item.name());
    }
}
