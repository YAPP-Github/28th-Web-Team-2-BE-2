package com.example.demo.report.application.query;

public record StoreReportsQuery(Long storeId, ReportFilter filter, int page, int size) {}
