package com.example.demo.report.presentation.dto;

import java.util.List;

public record RegionLowestPriceReportsResponse(
        String regionName, List<RegionLowestPriceReportResponse> items) {}
