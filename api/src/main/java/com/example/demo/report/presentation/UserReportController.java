package com.example.demo.report.presentation;

import com.example.demo.common.security.AuthPrincipal;
import com.example.demo.report.application.result.CreateUserReportResult;
import com.example.demo.report.application.usecase.CreateUserReportUseCase;
import com.example.demo.report.application.usecase.GetStoreReportsUseCase;
import com.example.demo.report.presentation.converter.UserReportCommandConverter;
import com.example.demo.report.presentation.converter.UserReportResultConverter;
import com.example.demo.report.presentation.converter.UserReportQueryConverter;
import com.example.demo.report.presentation.dto.CreateUserReportRequest;
import com.example.demo.report.presentation.dto.CreateUserReportResponse;
import com.example.demo.report.presentation.dto.StoreReportsRequest;
import com.example.demo.report.presentation.dto.StoreReportsResponse;
import com.example.demo.report.presentation.spec.UserReportControllerSpec;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class UserReportController implements UserReportControllerSpec {

    private final CreateUserReportUseCase createUserReportUseCase;
    private final GetStoreReportsUseCase getStoreReportsUseCase;
    private final UserReportCommandConverter commandConverter;
    private final UserReportQueryConverter queryConverter;
    private final UserReportResultConverter resultConverter;

    @PostMapping("/items/{itemId}/reports")
    @Override
    public ResponseEntity<CreateUserReportResponse> createReport(
            @PathVariable final Long itemId,
            @Valid @RequestBody final CreateUserReportRequest request,
            @AuthenticationPrincipal final AuthPrincipal principal) {
        final CreateUserReportResult result = createUserReportUseCase.execute(
                commandConverter.toCommand(itemId, principal.userId(), request));
        final CreateUserReportResponse response = resultConverter.toResponse(result);
        return ResponseEntity.created(UriComponentsBuilder.fromPath("/api/v1/items/{itemId}/reports/{reportId}")
                        .buildAndExpand(itemId, response.reportId()).toUri()).body(response);
    }

    @GetMapping("/stores/{storeId}/reports")
    @Override
    public ResponseEntity<StoreReportsResponse> getStoreReports(
            @PathVariable @Positive final Long storeId,
            @Valid @ModelAttribute final StoreReportsRequest request) {
        final StoreReportsResponse response = resultConverter.toStoreReportsResponse(
                getStoreReportsUseCase.execute(queryConverter.toStoreReportsQuery(storeId, request)));
        return ResponseEntity.ok(response);
    }
}
