package com.example.demo.report.application.query;

public record RegionItemReportQuery(
        String regionId, Long itemId, UserReportSort sort, int page, int size) {}
