package com.example.demo.report.presentation.converter;

import com.example.demo.report.application.result.CreateUserReportResult;
import com.example.demo.report.presentation.dto.CreateUserReportResponse;
import org.springframework.stereotype.Component;

@Component
public class UserReportResultConverter {
    public CreateUserReportResponse toResponse(final CreateUserReportResult result) {
        return new CreateUserReportResponse(result.reportId());
    }
}
