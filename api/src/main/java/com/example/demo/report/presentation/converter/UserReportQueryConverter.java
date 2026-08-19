package com.example.demo.report.presentation.converter;

import com.example.demo.report.application.query.StoreReportsQuery;
import com.example.demo.report.presentation.dto.StoreReportsRequest;
import org.springframework.stereotype.Component;

@Component
public class UserReportQueryConverter {

    public StoreReportsQuery toStoreReportsQuery(
            final Long storeId, final StoreReportsRequest request) {
        return new StoreReportsQuery(storeId, request.filter(), request.page(), request.size());
    }
}
