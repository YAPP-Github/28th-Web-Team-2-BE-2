package com.example.demo.report.application.usecase;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import com.example.demo.report.application.port.StoreReportQueryPort;
import com.example.demo.report.application.port.UserReportQueryPort;
import com.example.demo.report.application.query.StoreReportsQuery;
import com.example.demo.report.application.result.PriceClassification;
import com.example.demo.report.application.result.StoreReportResult;
import com.example.demo.report.application.result.StoreReportSource;
import com.example.demo.report.application.result.StoreReportsQueryResult;
import com.example.demo.report.application.result.StoreReportsResult;
import com.example.demo.user.domain.UserRank;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class GetStoreReportsUseCase {

    private static final String DELETED_REPORTER_NICKNAME = "탈퇴한 이웃";
    private static final List<String> PROFILE_COLORS = List.of("GREEN", "BLUE", "ORANGE", "GRAY");

    private final StoreReportQueryPort storeReportQueryPort;
    private final UserReportQueryPort userReportQueryPort;

    @Transactional(readOnly = true)
    public StoreReportsResult execute(final StoreReportsQuery query) {
        log.info(
                "store reports query started storeId={} filter={} page={} size={}",
                query.storeId(), query.filter(), query.page(), query.size());
        final StoreReportsQueryResult result = storeReportQueryPort.find(query);
        if (!result.storeExists()) {
            throw new ApiException(
                    ErrorType.NO_RESOURCE_ERROR.description(),
                    ErrorType.NO_RESOURCE_ERROR,
                    HttpStatus.NOT_FOUND);
        }
        final Set<Long> reporterIds = result.reports().stream()
                .map(StoreReportSource::reporterId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        final Map<Long, Long> reportCounts = userReportQueryPort.findReportCounts(reporterIds);
        final List<StoreReportResult> reports = result.reports().stream()
                .map(source -> toResult(source, reportCounts))
                .toList();
        final StoreReportsResult response = new StoreReportsResult(
                query.storeId(), result.cheapCount(), result.expensiveCount(), reports,
                query.page(), query.size(), result.hasNext());
        log.info(
                "store reports query completed storeId={} resultCount={} cheapCount={} expensiveCount={}",
                query.storeId(), reports.size(), result.cheapCount(), result.expensiveCount());
        return response;
    }

    private StoreReportResult toResult(
            final StoreReportSource source, final Map<Long, Long> reportCounts) {
        return new StoreReportResult(
                source.reportId(), source.itemId(), source.itemName(), source.itemImageUrl(),
                source.price(), source.unit(), source.reportedDate(), source.publicPriceDiff(),
                source.priceDiffRate(), classify(source.publicPriceDiff()), reporterNickname(source),
                reporterRank(source.reporterId(), reportCounts),
                reporterProfileColor(source.reporterId()));
    }

    private UserRank reporterRank(final Long reporterId, final Map<Long, Long> reportCounts) {
        if (reporterId == null) {
            return null;
        }
        return UserRank.fromReportCount(reportCounts.getOrDefault(reporterId, 0L));
    }

    private String reporterNickname(final StoreReportSource source) {
        if (source.reporterId() == null) {
            return DELETED_REPORTER_NICKNAME;
        }
        return source.reporterNickname();
    }

    private String reporterProfileColor(final Long reporterId) {
        if (reporterId == null) {
            return null;
        }
        return PROFILE_COLORS.get(Math.floorMod(reporterId, PROFILE_COLORS.size()));
    }

    private PriceClassification classify(final Integer priceDiff) {
        if (priceDiff == null) {
            return null;
        }
        if (priceDiff < 0) {
            return PriceClassification.CHEAP;
        }
        if (priceDiff > 0) {
            return PriceClassification.EXPENSIVE;
        }
        return PriceClassification.EQUAL;
    }
}
