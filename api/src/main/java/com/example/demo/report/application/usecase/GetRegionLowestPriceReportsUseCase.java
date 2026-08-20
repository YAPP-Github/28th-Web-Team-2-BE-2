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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetRegionLowestPriceReportsUseCase {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private final RegionLowestPriceReportQueryPort regionLowestPriceReportQueryPort;

    @Transactional(readOnly = true)
    public RegionLowestPriceReportsResult execute(final RegionLowestPriceReportsQuery query) {
        final LocalDate today = LocalDate.now(SEOUL);
        final RegionLowestPriceReportsQueryResult result = regionLowestPriceReportQueryPort.find(
                query.regionId(), today.minusDays(6), today);
        if (!result.regionExists()) {
            throw new ApiException(
                    ErrorType.NO_RESOURCE_ERROR.description(),
                    ErrorType.NO_RESOURCE_ERROR,
                    HttpStatus.NOT_FOUND);
        }
        return new RegionLowestPriceReportsResult(
                result.regionName(), toResults(result.sources(), query.limit()));
    }

    private List<RegionLowestPriceReportResult> toResults(
            final List<RegionLowestPriceReportSource> sources, final int limit) {
        final Map<Long, RegionLowestPriceReportSource> lowestByItem = new LinkedHashMap<>();
        sources.forEach(source -> lowestByItem.putIfAbsent(source.itemId(), source));
        final List<RegionLowestPriceReportSource> selected = lowestByItem.values().stream()
                .sorted(Comparator.comparing(RegionLowestPriceReportSource::priceDiffRate)
                        .thenComparing(RegionLowestPriceReportSource::price)
                        .thenComparing(RegionLowestPriceReportSource::reportedAt, Comparator.reverseOrder())
                        .thenComparing(RegionLowestPriceReportSource::reportId, Comparator.reverseOrder()))
                .limit(limit)
                .toList();
        return IntStream.range(0, selected.size())
                .mapToObj(index -> toResult(selected.get(index), index + 1))
                .toList();
    }

    private RegionLowestPriceReportResult toResult(
            final RegionLowestPriceReportSource source, final int rank) {
        return new RegionLowestPriceReportResult(
                rank, source.reportId(), source.itemId(), source.itemName(), source.itemImageUrl(),
                source.storeId(), source.storeName(), source.price(), source.unit(), source.priceDiffRate());
    }
}
