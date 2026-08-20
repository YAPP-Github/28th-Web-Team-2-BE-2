package com.example.demo.report.application.usecase;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import com.example.demo.report.application.port.UserReportCommandPort;
import com.example.demo.report.application.port.UserReportQueryPort;
import com.example.demo.report.domain.UserReport;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeleteUserReportUseCase {

    private final UserReportQueryPort userReportQueryPort;
    private final UserReportCommandPort userReportCommandPort;

    @Transactional
    public void execute(final Long reportId, final Long userId) {
        final UserReport report = userReportQueryPort
                .findByIdAndUserId(reportId, userId)
                .orElseThrow(this::reportNotFound);
        userReportCommandPort.delete(report);
    }

    private ApiException reportNotFound() {
        return new ApiException(
                ErrorType.NO_RESOURCE_ERROR.description(),
                ErrorType.NO_RESOURCE_ERROR,
                HttpStatus.NOT_FOUND);
    }
}
