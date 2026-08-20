package com.example.demo.report.infrastructure;

import com.example.demo.item.domain.QItem;
import com.example.demo.region.domain.QRegion;
import com.example.demo.report.application.port.RegionLowestPriceReportQueryPort;
import com.example.demo.report.application.result.RegionLowestPriceReportSource;
import com.example.demo.report.application.result.RegionLowestPriceReportsQueryResult;
import com.example.demo.report.domain.QStore;
import com.example.demo.report.domain.QUserReport;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RegionLowestPriceReportQueryAdapter implements RegionLowestPriceReportQueryPort {

    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public RegionLowestPriceReportsQueryResult find(
            final String regionId, final LocalDate from, final LocalDate to) {
        final QRegion region = QRegion.region;
        final String regionName = jpaQueryFactory
                .select(region.regionName)
                .from(region)
                .where(region.regionId.eq(regionId))
                .fetchOne();
        if (regionName == null) {
            return new RegionLowestPriceReportsQueryResult(false, null, List.of());
        }

        final QUserReport report = QUserReport.userReport;
        final QItem item = QItem.item;
        final QStore store = QStore.store;
        final List<RegionLowestPriceReportSource> sources = jpaQueryFactory
                .select(Projections.constructor(
                        RegionLowestPriceReportSource.class,
                        report.id,
                        report.itemId,
                        item.name,
                        item.imageUrl,
                        report.storeId,
                        store.placeName,
                        report.price,
                        report.unit,
                        report.priceDiffRate,
                        report.reportDate))
                .from(report)
                .join(item).on(item.id.eq(report.itemId))
                .leftJoin(store).on(store.id.eq(report.storeId))
                .where(
                        report.regionId.eq(regionId),
                        report.reportDate.between(from, to),
                        report.unit.eq(item.defaultUnit),
                        report.publicPriceDiff.lt(0),
                        report.priceDiffRate.isNotNull())
                .orderBy(
                        report.price.asc(),
                        report.reportDate.desc(),
                        report.id.desc())
                .fetch();
        return new RegionLowestPriceReportsQueryResult(true, regionName, sources);
    }
}
