package com.example.demo.report.infrastructure;

import com.example.demo.report.application.port.UserReportQueryPort;
import com.example.demo.report.domain.UserReport;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
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
}
