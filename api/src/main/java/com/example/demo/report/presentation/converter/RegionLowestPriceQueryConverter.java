package com.example.demo.report.presentation.converter;

import com.example.demo.report.application.query.RegionLowestPriceReportsQuery;
import com.example.demo.report.presentation.dto.RegionLowestPriceReportsRequest;
import org.springframework.stereotype.Component;

@Component
public class RegionLowestPriceQueryConverter {

    public RegionLowestPriceReportsQuery toQuery(
            final String regionId, final RegionLowestPriceReportsRequest request) {
        return new RegionLowestPriceReportsQuery(regionId, request.limit());
    }
}
