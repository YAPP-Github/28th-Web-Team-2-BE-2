package com.example.demo.report.application.usecase;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import com.example.demo.report.application.port.UserReportCommandPort;
import com.example.demo.report.application.port.UserReportQueryPort;
import com.example.demo.report.domain.UserReport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeleteUserReportUseCase {

    private final UserReportQueryPort userReportQueryPort;
    private final UserReportCommandPort userReportCommandPort;

    @Transactional
    public void execute(final Long reportId, final Long userId) {
        log.info("user report delete started reportId={} userId={}", reportId, userId);
        final UserReport report = userReportQueryPort
                .findByIdAndUserId(reportId, userId)
                .orElseThrow(this::reportNotFound);
        userReportCommandPort.delete(report);
        log.info("user report deleted reportId={} userId={} itemId={}", reportId, userId, report.itemId());
    }

    private ApiException reportNotFound() {
        return new ApiException(
                ErrorType.NO_RESOURCE_ERROR.description(),
                ErrorType.NO_RESOURCE_ERROR,
                HttpStatus.NOT_FOUND);
    }
}
