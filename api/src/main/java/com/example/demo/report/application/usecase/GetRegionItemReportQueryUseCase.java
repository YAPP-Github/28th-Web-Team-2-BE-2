package com.example.demo.report.application.usecase;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import com.example.demo.report.application.contract.ItemCandidate;
import com.example.demo.report.application.port.ItemCandidateQueryPort;
import com.example.demo.report.application.port.RegionNameQueryPort;
import com.example.demo.report.application.port.StoreNameQueryPort;
import com.example.demo.report.application.port.UserReportQueryPort;
import com.example.demo.report.application.query.RegionItemReportQuery;
import com.example.demo.report.application.result.PriceClassification;
import com.example.demo.report.application.result.RegionItemReportResult;
import com.example.demo.report.application.result.UserReportSummaryResult;
import com.example.demo.report.domain.UserReport;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class GetRegionItemReportQueryUseCase {

    private final ItemCandidateQueryPort itemCandidateQueryPort;
    private final UserReportQueryPort userReportQueryPort;
    private final StoreNameQueryPort storeNameQueryPort;
    private final RegionNameQueryPort regionNameQueryPort;

    @Transactional(readOnly = true)
    public RegionItemReportResult execute(final RegionItemReportQuery query) {
        log.info(
                "region item reports query started regionId={} itemId={} page={} size={}",
                query.regionId(), query.itemId(), query.page(), query.size());
        final ItemCandidate item = findItem(query.itemId());
        final String regionName = regionName(query.regionId());
        final Page<UserReport> reports =
                userReportQueryPort.findByRegionAndItem(query, item.defaultUnit());
        final Map<Long, String> storeNames = findStoreNames(reports.getContent());
        final RegionItemReportResult result = new RegionItemReportResult(
                query.regionId(),
                regionName,
                item.itemId(),
                reports.getTotalElements(),
                toSummaries(reports.getContent(), storeNames),
                query.page(),
                query.size(),
                reports.hasNext());
        log.info(
                "region item reports query completed regionId={} itemId={} resultCount={} totalCount={}",
                query.regionId(), query.itemId(), reports.getNumberOfElements(), reports.getTotalElements());
        return result;
    }

    /** 법정동 코드에 해당하는 지역명이다. 참조 데이터에 없는 코드는 조회할 수 없다. */
    private String regionName(final String regionId) {
        return regionNameQueryPort.findName(regionId).orElseThrow(this::noResource);
    }

    private ItemCandidate findItem(final Long itemId) {
        return itemCandidateQueryPort.findById(itemId).orElseThrow(this::noResource);
    }

    private ApiException noResource() {
        return new ApiException(
                ErrorType.NO_RESOURCE_ERROR.description(),
                ErrorType.NO_RESOURCE_ERROR,
                HttpStatus.NOT_FOUND);
    }

    private Map<Long, String> findStoreNames(final List<UserReport> reports) {
        final List<Long> storeIds = reports.stream()
                .map(UserReport::storeId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        return storeNameQueryPort.findNames(storeIds);
    }

    private List<UserReportSummaryResult> toSummaries(
            final List<UserReport> reports, final Map<Long, String> storeNames) {
        return reports.stream()
                .map(report -> toSummary(report, storeName(report, storeNames)))
                .toList();
    }

    private String storeName(final UserReport report, final Map<Long, String> storeNames) {
        if (report.storeId() == null) {
            return null;
        }
        return storeNames.get(report.storeId());
    }

    private UserReportSummaryResult toSummary(final UserReport report, final String storeName) {
        return new UserReportSummaryResult(
                report.id(),
                report.storeId(),
                storeName,
                report.price(),
                report.amount(),
                report.unit(),
                report.reportDate(),
                report.publicPriceDiff(),
                report.priceDiffRate(),
                PriceClassification.of(report.publicPriceDiff()));
    }
}
