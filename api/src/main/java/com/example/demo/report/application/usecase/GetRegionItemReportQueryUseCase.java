package com.example.demo.report.application.usecase;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import com.example.demo.item.application.port.ItemExistencePort;
import com.example.demo.item.domain.Item;
import com.example.demo.report.application.port.StoreNameQueryPort;
import com.example.demo.user.application.port.RegionReferenceRepository;
import com.example.demo.report.application.port.UserReportQueryPort;
import com.example.demo.report.application.query.RegionItemReportQuery;
import com.example.demo.report.application.result.RegionItemReportResult;
import com.example.demo.report.application.result.UserReportSummaryResult;
import com.example.demo.report.domain.PriceClassification;
import com.example.demo.report.domain.UserReport;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetRegionItemReportQueryUseCase {

    private final ItemExistencePort itemExistencePort;
    private final UserReportQueryPort userReportQueryPort;
    private final StoreNameQueryPort storeNameQueryPort;
    private final RegionReferenceRepository regionReferenceRepository;

    @Transactional(readOnly = true)
    public RegionItemReportResult execute(final RegionItemReportQuery query) {
        final String regionName = findRegionName(query.regionId());
        final Item item = findItem(query.itemId());
        final Page<UserReport> reports =
                userReportQueryPort.findByRegionAndItem(query, item.defaultUnit());
        final Map<Long, String> storeNames = findStoreNames(reports.getContent());
        return new RegionItemReportResult(
                query.regionId(),
                regionName,
                item.id(),
                reports.getTotalElements(),
                toSummaries(reports.getContent(), storeNames),
                query.page(),
                query.size(),
                reports.hasNext());
    }

    /**
     * 법정동 코드의 지역명이다.
     *
     * <p>참조 데이터에 없는 코드는 404다. 없는 품목과 같은 취급이며, 오타난 코드와 "제보가 없는 동네"를 구분할 수 있게 한다. 빈 목록은
     * 지역이 존재하고 제보만 없을 때의 응답이다.
     */
    private String findRegionName(final String regionId) {
        final String regionName =
                regionReferenceRepository.findNamesByIds(List.of(regionId)).get(regionId);
        if (regionName == null) {
            throw new ApiException(
                    ErrorType.NO_RESOURCE_ERROR.description(),
                    ErrorType.NO_RESOURCE_ERROR,
                    HttpStatus.NOT_FOUND);
        }
        return regionName;
    }

    private Item findItem(final Long itemId) {
        return itemExistencePort.findById(itemId).orElseThrow(() -> new ApiException(
                ErrorType.NO_RESOURCE_ERROR.description(),
                ErrorType.NO_RESOURCE_ERROR,
                HttpStatus.NOT_FOUND));
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
