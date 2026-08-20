package com.example.demo.report.application.result;

import java.util.List;

public record RegionLowestPriceReportsResult(
        String regionName, List<RegionLowestPriceReportResult> items) {}
