package com.example.demo.report.presentation.converter;

import com.example.demo.report.application.result.CreateUserReportResult;
import com.example.demo.report.application.result.StoreReportResult;
import com.example.demo.report.application.result.StoreReportsResult;
import com.example.demo.report.presentation.dto.CreateUserReportResponse;
import com.example.demo.report.presentation.dto.StoreReportResponse;
import com.example.demo.report.presentation.dto.StoreReportsResponse;
import com.example.demo.report.presentation.dto.StoreReportsSummaryResponse;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class UserReportResultConverter {
    public CreateUserReportResponse toResponse(final CreateUserReportResult result) {
        return new CreateUserReportResponse(result.reportId(), result.itemId(), result.storeId(), result.reportedAt());
    }

    public StoreReportsResponse toStoreReportsResponse(final StoreReportsResult result) {
        final List<StoreReportResponse> reports = result.reports().stream()
                .map(this::toStoreReportResponse)
                .toList();
        return new StoreReportsResponse(
                result.storeId(),
                new StoreReportsSummaryResponse(result.cheapCount(), result.expensiveCount()),
                reports,
                result.page(),
                result.size(),
                result.hasNext());
    }

    private StoreReportResponse toStoreReportResponse(final StoreReportResult result) {
        return new StoreReportResponse(
                result.reportId(), result.itemId(), result.itemName(), result.itemImageUrl(),
                result.price(), result.unit(), result.reportedDate(), result.publicPriceDiff(),
                result.priceDiffRate(), result.priceClassification());
    }
}
