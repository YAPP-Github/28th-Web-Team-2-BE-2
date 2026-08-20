package com.example.demo.report.presentation;

import com.example.demo.common.security.AuthPrincipal;
import com.example.demo.report.application.result.MyReportPageResult;
import com.example.demo.report.application.result.MyWeeklyReportResult;
import com.example.demo.report.application.usecase.DeleteUserReportUseCase;
import com.example.demo.report.application.usecase.GetMyReportQueryUseCase;
import com.example.demo.report.application.usecase.GetMyWeeklyReportQueryUseCase;
import com.example.demo.report.application.usecase.UpdateUserReportUseCase;
import com.example.demo.report.presentation.converter.UserReportCommandConverter;
import com.example.demo.report.presentation.converter.UserReportResultConverter;
import com.example.demo.report.presentation.dto.MyReportPageResponse;
import com.example.demo.report.presentation.dto.MyReportRequest;
import com.example.demo.report.presentation.dto.MyWeeklyReportResponse;
import com.example.demo.report.presentation.dto.UpdateUserReportRequest;
import com.example.demo.report.presentation.spec.MyReportControllerSpec;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me/reports")
@RequiredArgsConstructor
public class MyReportController implements MyReportControllerSpec {

    private final GetMyReportQueryUseCase getMyReportQueryUseCase;
    private final GetMyWeeklyReportQueryUseCase getMyWeeklyReportQueryUseCase;
    private final UpdateUserReportUseCase updateUserReportUseCase;
    private final DeleteUserReportUseCase deleteUserReportUseCase;
    private final UserReportCommandConverter userReportCommandConverter;
    private final UserReportResultConverter userReportResultConverter;

    @GetMapping
    @Override
    public ResponseEntity<MyReportPageResponse> getMyReports(
            @Valid @ModelAttribute final MyReportRequest request,
            @AuthenticationPrincipal final AuthPrincipal principal) {
        final MyReportPageResult result = getMyReportQueryUseCase.execute(
                userReportResultConverter.toMyReportQuery(principal.userId(), request));
        return ResponseEntity.ok(userReportResultConverter.toMyReportPageResponse(result));
    }

    @GetMapping("/weekly")
    @Override
    public ResponseEntity<MyWeeklyReportResponse> getMyWeeklyReports(
            @AuthenticationPrincipal final AuthPrincipal principal) {
        final MyWeeklyReportResult result =
                getMyWeeklyReportQueryUseCase.execute(principal.userId());
        return ResponseEntity.ok(userReportResultConverter.toMyWeeklyReportResponse(result));
    }

    @PatchMapping("/{reportId}")
    @Override
    public ResponseEntity<Void> updateMyReport(
            @Positive @PathVariable final Long reportId,
            @Valid @RequestBody final UpdateUserReportRequest request,
            @AuthenticationPrincipal final AuthPrincipal principal) {
        updateUserReportUseCase.execute(
                userReportCommandConverter.toUpdateCommand(reportId, principal.userId(), request));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{reportId}")
    @Override
    public ResponseEntity<Void> deleteMyReport(
            @Positive @PathVariable final Long reportId,
            @AuthenticationPrincipal final AuthPrincipal principal) {
        deleteUserReportUseCase.execute(reportId, principal.userId());
        return ResponseEntity.noContent().build();
    }
}
