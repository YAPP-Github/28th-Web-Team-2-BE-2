package com.example.demo.report.infrastructure;

import com.example.demo.item.domain.QItem;
import com.example.demo.report.application.port.StoreReportQueryPort;
import com.example.demo.report.application.query.ReportFilter;
import com.example.demo.report.application.query.StoreReportsQuery;
import com.example.demo.report.application.result.StoreReportSource;
import com.example.demo.report.application.result.StoreReportsQueryResult;
import com.example.demo.report.domain.QStore;
import com.example.demo.report.domain.QUserReport;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class StoreReportQueryAdapter implements StoreReportQueryPort {

    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public StoreReportsQueryResult find(final StoreReportsQuery query) {
        final QStore store = QStore.store;
        final QUserReport report = QUserReport.userReport;
        final QItem item = QItem.item;
        final BooleanExpression baseCondition = report.storeId.eq(query.storeId())
                .and(report.unit.eq(item.defaultUnit));
        final BooleanExpression filterCondition = filterCondition(report, query.filter());
        final List<StoreReportSource> reports = jpaQueryFactory
                .select(Projections.constructor(
                        StoreReportSource.class,
                        report.id,
                        report.itemId,
                        item.name,
                        item.imageUrl,
                        report.price,
                        report.unit,
                        report.reportDate,
                        report.publicPriceDiff,
                        report.priceDiffRate))
                .from(report)
                .join(item).on(item.id.eq(report.itemId))
                .where(baseCondition, filterCondition)
                .orderBy(report.reportDate.desc(), report.id.desc())
                .offset((long) query.page() * query.size())
                .limit(query.size() + 1L)
                .fetch();
        final boolean hasNext = reports.size() > query.size();
        final List<StoreReportSource> page = pageContent(reports, hasNext, query.size());
        final long cheapCount = count(report, item, baseCondition, report.publicPriceDiff.lt(0));
        final long expensiveCount = count(report, item, baseCondition, report.publicPriceDiff.gt(0));
        final boolean storeExists = jpaQueryFactory
                .selectOne()
                .from(store)
                .where(store.id.eq(query.storeId()))
                .fetchFirst() != null;
        return new StoreReportsQueryResult(storeExists, cheapCount, expensiveCount, page, hasNext);
    }

    private long count(
            final QUserReport report,
            final QItem item,
            final BooleanExpression baseCondition,
            final BooleanExpression classification) {
        final Long count = jpaQueryFactory
                .select(report.count())
                .from(report)
                .join(item).on(item.id.eq(report.itemId))
                .where(baseCondition, classification)
                .fetchOne();
        if (count == null) {
            return 0L;
        }
        return count;
    }

    private List<StoreReportSource> pageContent(
            final List<StoreReportSource> reports, final boolean hasNext, final int size) {
        if (hasNext) {
            return reports.subList(0, size);
        }
        return reports;
    }

    private BooleanExpression filterCondition(final QUserReport report, final ReportFilter filter) {
        if (filter == ReportFilter.CHEAP) {
            return report.publicPriceDiff.lt(0);
        }
        if (filter == ReportFilter.EXPENSIVE) {
            return report.publicPriceDiff.gt(0);
        }
        return null;
    }
}
