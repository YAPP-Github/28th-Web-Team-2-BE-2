package com.example.demo.report.application.usecase;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import com.example.demo.report.application.port.StoreReportQueryPort;
import com.example.demo.report.application.query.StoreReportsQuery;
import com.example.demo.report.application.result.PriceClassification;
import com.example.demo.report.application.result.StoreReportResult;
import com.example.demo.report.application.result.StoreReportSource;
import com.example.demo.report.application.result.StoreReportsQueryResult;
import com.example.demo.report.application.result.StoreReportsResult;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetStoreReportsUseCase {

    private final StoreReportQueryPort storeReportQueryPort;

    @Transactional(readOnly = true)
    public StoreReportsResult execute(final StoreReportsQuery query) {
        final StoreReportsQueryResult result = storeReportQueryPort.find(query);
        if (!result.storeExists()) {
            throw new ApiException(
                    ErrorType.NO_RESOURCE_ERROR.description(),
                    ErrorType.NO_RESOURCE_ERROR,
                    HttpStatus.NOT_FOUND);
        }
        final List<StoreReportResult> reports = result.reports().stream()
                .map(this::toResult)
                .toList();
        return new StoreReportsResult(
                query.storeId(), result.cheapCount(), result.expensiveCount(), reports,
                query.page(), query.size(), result.hasNext());
    }

    private StoreReportResult toResult(final StoreReportSource source) {
        return new StoreReportResult(
                source.reportId(), source.itemId(), source.itemName(), source.itemImageUrl(),
                source.price(), source.unit(), source.reportedDate(), source.publicPriceDiff(),
                source.priceDiffRate(), classify(source.publicPriceDiff()));
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
