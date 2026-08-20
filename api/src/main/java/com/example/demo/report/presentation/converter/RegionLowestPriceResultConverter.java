package com.example.demo.report.presentation.converter;

import com.example.demo.report.application.result.RegionLowestPriceReportResult;
import com.example.demo.report.application.result.RegionLowestPriceReportsResult;
import com.example.demo.report.presentation.dto.RegionLowestPriceReportResponse;
import com.example.demo.report.presentation.dto.RegionLowestPriceReportsResponse;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RegionLowestPriceResultConverter {

    public RegionLowestPriceReportsResponse toResponse(final RegionLowestPriceReportsResult result) {
        final List<RegionLowestPriceReportResponse> items = result.items().stream()
                .map(this::toItemResponse)
                .toList();
        return new RegionLowestPriceReportsResponse(result.regionName(), items);
    }

    private RegionLowestPriceReportResponse toItemResponse(
            final RegionLowestPriceReportResult result) {
        return new RegionLowestPriceReportResponse(
                result.rank(), result.reportId(), result.itemId(), result.itemName(), result.itemImageUrl(),
                result.storeId(), result.storeName(), result.price(), result.unit(), result.priceDiffRate());
    }
}
