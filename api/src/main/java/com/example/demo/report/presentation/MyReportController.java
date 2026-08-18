package com.example.demo.report.presentation;

import com.example.demo.common.security.AuthPrincipal;
import com.example.demo.report.application.result.MyReportPageResult;
import com.example.demo.report.application.usecase.GetMyReportQueryUseCase;
import com.example.demo.report.presentation.converter.UserReportResultConverter;
import com.example.demo.report.presentation.dto.MyReportPageResponse;
import com.example.demo.report.presentation.dto.MyReportRequest;
import com.example.demo.report.presentation.spec.MyReportControllerSpec;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me/reports")
@RequiredArgsConstructor
public class MyReportController implements MyReportControllerSpec {

    private final GetMyReportQueryUseCase getMyReportQueryUseCase;
    private final UserReportResultConverter userReportResultConverter;

    @GetMapping
    @Override
    public ResponseEntity<MyReportPageResponse> getMyReports(
            @Valid @ModelAttribute final MyReportRequest request,
            @AuthenticationPrincipal final AuthPrincipal principal) {
        final MyReportPageResult result = getMyReportQueryUseCase.execute(
                userReportResultConverter.toQuery(principal.userId(), request));
        return ResponseEntity.ok(userReportResultConverter.toResponse(result));
    }
}
