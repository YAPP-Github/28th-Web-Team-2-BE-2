package com.example.demo.report.infrastructure;

import com.example.demo.report.application.port.UserReportQueryPort;
import com.example.demo.report.application.query.RegionItemReportQuery;
import com.example.demo.report.application.query.UserReportSort;
import com.example.demo.report.domain.Store;
import com.example.demo.report.domain.UserReport;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserReportQueryAdapter implements UserReportQueryPort {

    private final UserReportJpaRepository userReportJpaRepository;
    private final ReportStoreJpaRepository storeJpaRepository;

    @Override
    public Optional<Integer> findLatestPrice(
            final Long itemId, final String regionId, final String unit) {
        return userReportJpaRepository
                .findFirstByItemIdAndRegionIdAndUnitOrderByReportDateDescIdDesc(
                        itemId, regionId, unit)
                .map(UserReport::price);
    }

    @Override
    public Page<UserReport> findByRegionAndItem(
            final RegionItemReportQuery query, final String unit) {
        return userReportJpaRepository.findAllByItemIdAndRegionIdAndUnit(
                query.itemId(), query.regionId(), unit, pageable(query));
    }

    @Override
    public Map<Long, String> findStoreNames(final Collection<Long> storeIds) {
        if (storeIds.isEmpty()) {
            return Map.of();
        }
        return storeJpaRepository.findAllByIdIn(storeIds).stream()
                .collect(Collectors.toMap(Store::id, Store::placeName));
    }

    private Pageable pageable(final RegionItemReportQuery query) {
        return PageRequest.of(query.page(), query.size(), sort(query.sort()));
    }

    private Sort sort(final UserReportSort sort) {
        if (sort == UserReportSort.PRICE_ASC) {
            return Sort.by(Sort.Order.asc("price"), Sort.Order.asc("id"));
        }
        return Sort.by(Sort.Order.desc("reportDate"), Sort.Order.desc("id"));
    }
}
