package com.example.demo.report.application.usecase;

import com.example.demo.item.application.port.ItemExistencePort;
import com.example.demo.report.application.port.UserReportQueryPort;
import com.example.demo.user.application.port.RegionReferenceRepository;
import com.example.demo.report.application.query.MyReportQuery;
import com.example.demo.report.application.result.MyReportPageResult;
import com.example.demo.report.application.result.MyReportSummaryResult;
import com.example.demo.report.domain.UserReport;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetMyReportQueryUseCase {

    private final UserReportQueryPort userReportQueryPort;
    private final ItemExistencePort itemExistencePort;
    private final RegionReferenceRepository regionReferenceRepository;

    @Transactional(readOnly = true)
    public MyReportPageResult execute(final MyReportQuery query) {
        final Page<UserReport> reports = userReportQueryPort.findByUser(query);
        final Map<Long, String> itemNames = findItemNames(reports.getContent());
        final Map<String, String> regionNames = findRegionNames(reports.getContent());
        return new MyReportPageResult(
                toSummaries(reports.getContent(), itemNames, regionNames),
                query.page(),
                query.size(),
                reports.getTotalElements(),
                reports.getTotalPages(),
                reports.hasNext());
    }

    private String regionName(
            final UserReport report, final Map<String, String> regionNames) {
        if (report.regionId() == null) {
            return null;
        }
        return regionNames.get(report.regionId());
    }

    private Map<Long, String> findItemNames(final List<UserReport> reports) {
        final List<Long> itemIds = reports.stream().map(UserReport::itemId).toList();
        return itemExistencePort.findNamesByIds(itemIds);
    }

    /** 법정동 코드별 지역명이다. 참조 데이터에 없는 코드는 결과에 담기지 않는다. */
    private Map<String, String> findRegionNames(final List<UserReport> reports) {
        final List<String> regionIds = reports.stream()
                .map(UserReport::regionId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        return regionReferenceRepository.findNamesByIds(regionIds);
    }

    private List<MyReportSummaryResult> toSummaries(
            final List<UserReport> reports,
            final Map<Long, String> itemNames,
            final Map<String, String> regionNames) {
        return reports.stream()
                .map(report -> toSummary(report, itemNames, regionNames))
                .toList();
    }

    private MyReportSummaryResult toSummary(
            final UserReport report,
            final Map<Long, String> itemNames,
            final Map<String, String> regionNames) {
        return new MyReportSummaryResult(
                report.id(),
                itemNames.get(report.itemId()),
                report.price(),
                report.unit(),
                report.reportDate(),
                report.regionId(),
                regionName(report, regionNames),
                report.publicPriceDiff());
    }
}
