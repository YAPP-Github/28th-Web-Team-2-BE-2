package com.example.demo.report.application.port;

import com.example.demo.report.application.query.StoreReportsQuery;
import com.example.demo.report.application.result.StoreReportsQueryResult;

public interface StoreReportQueryPort {

    StoreReportsQueryResult find(StoreReportsQuery query);
}
