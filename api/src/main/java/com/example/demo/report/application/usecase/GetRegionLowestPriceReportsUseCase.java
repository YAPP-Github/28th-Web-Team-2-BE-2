package com.example.demo.report.application.usecase;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import com.example.demo.report.application.port.RegionLowestPriceReportQueryPort;
import com.example.demo.report.application.query.RegionLowestPriceReportsQuery;
import com.example.demo.report.application.result.RegionLowestPriceReportResult;
import com.example.demo.report.application.result.RegionLowestPriceReportSource;
import com.example.demo.report.application.result.RegionLowestPriceReportsQueryResult;
import com.example.demo.report.application.result.RegionLowestPriceReportsResult;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class GetRegionLowestPriceReportsUseCase {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private final RegionLowestPriceReportQueryPort regionLowestPriceReportQueryPort;

    @Transactional(readOnly = true)
    public RegionLowestPriceReportsResult execute(final RegionLowestPriceReportsQuery query) {
        log.info(
                "region lowest price reports query started regionId={} limit={}",
                query.regionId(), query.limit());
        final LocalDate today = LocalDate.now(SEOUL);
        final RegionLowestPriceReportsQueryResult result = regionLowestPriceReportQueryPort.find(
                query.regionId(), today.minusDays(6), today);
        if (!result.regionExists()) {
            throw new ApiException(
                    ErrorType.NO_RESOURCE_ERROR.description(),
                    ErrorType.NO_RESOURCE_ERROR,
                    HttpStatus.NOT_FOUND);
        }
        final RegionLowestPriceReportsResult response = new RegionLowestPriceReportsResult(
                result.regionName(), toResults(result.sources(), query.limit()));
        log.info(
                "region lowest price reports query completed regionId={} resultCount={}",
                query.regionId(), response.items().size());
        return response;
    }

    private List<RegionLowestPriceReportResult> toResults(
            final List<RegionLowestPriceReportSource> sources, final int limit) {
        final Comparator<RegionLowestPriceReportSource> ranking =
                Comparator.comparing(RegionLowestPriceReportSource::priceDiffRate)
                        .thenComparing(RegionLowestPriceReportSource::price)
                        .thenComparing(RegionLowestPriceReportSource::reportedAt, Comparator.reverseOrder())
                        .thenComparing(RegionLowestPriceReportSource::reportId, Comparator.reverseOrder());
        final Comparator<RegionLowestPriceReportSource> lowestPrice =
                Comparator.comparing(RegionLowestPriceReportSource::price)
                        .thenComparing(RegionLowestPriceReportSource::reportedAt, Comparator.reverseOrder())
                        .thenComparing(RegionLowestPriceReportSource::reportId, Comparator.reverseOrder());
        final Map<Long, RegionLowestPriceReportSource> lowestByItem = new LinkedHashMap<>();
        sources.stream()
                .filter(source -> source.priceDiffRate() != null)
                .forEach(source -> putLowestPrice(lowestByItem, source, lowestPrice));
        final List<RegionLowestPriceReportSource> selected = lowestByItem.values().stream()
                .sorted(ranking)
                .limit(limit)
                .toList();
        return IntStream.range(0, selected.size())
                .mapToObj(index -> toResult(selected.get(index), index + 1))
                .toList();
    }

    private void putLowestPrice(
            final Map<Long, RegionLowestPriceReportSource> lowestByItem,
            final RegionLowestPriceReportSource candidate,
            final Comparator<RegionLowestPriceReportSource> lowestPrice) {
        final RegionLowestPriceReportSource existing = lowestByItem.get(candidate.itemId());
        if (existing == null || lowestPrice.compare(candidate, existing) < 0) {
            lowestByItem.put(candidate.itemId(), candidate);
        }
    }

    private RegionLowestPriceReportResult toResult(
            final RegionLowestPriceReportSource source, final int rank) {
        return new RegionLowestPriceReportResult(
                rank, source.reportId(), source.itemId(), source.itemName(), source.itemImageUrl(),
                source.storeId(), source.storeName(), source.price(), source.unit(), source.priceDiffRate());
    }
}
