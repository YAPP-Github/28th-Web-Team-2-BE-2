package com.example.demo.report.infrastructure;

import com.example.demo.report.application.port.UserReportQueryPort;
import com.example.demo.report.application.query.RegionItemReportQuery;
import com.example.demo.report.application.query.UserReportSort;
import com.example.demo.report.domain.UserReport;
import java.util.Optional;
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
