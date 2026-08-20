package com.example.demo.report.application.result;

import java.util.List;

public record RegionLowestPriceReportsQueryResult(
        boolean regionExists, String regionName, List<RegionLowestPriceReportSource> sources) {}
