package com.example.demo.report.presentation;

import com.example.demo.report.application.result.RegionItemReportResult;
import com.example.demo.report.application.usecase.GetRegionItemReportQueryUseCase;
import com.example.demo.report.presentation.converter.UserReportResultConverter;
import com.example.demo.report.presentation.dto.RegionItemReportRequest;
import com.example.demo.report.presentation.dto.RegionItemReportResponse;
import com.example.demo.report.presentation.spec.RegionItemReportControllerSpec;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/regions/{regionId}/items/{itemId}/reports")
@RequiredArgsConstructor
public class RegionItemReportController implements RegionItemReportControllerSpec {

    private final GetRegionItemReportQueryUseCase getRegionItemReportQueryUseCase;
    private final UserReportResultConverter userReportResultConverter;

    @GetMapping
    @Override
    public ResponseEntity<RegionItemReportResponse> getRegionItemReports(
            @Pattern(regexp = "\\d{10}") @PathVariable final String regionId,
            @Positive @PathVariable final Long itemId,
            @Valid @ModelAttribute final RegionItemReportRequest request) {
        final RegionItemReportResult result = getRegionItemReportQueryUseCase.execute(
                userReportResultConverter.toQuery(regionId, itemId, request));
        return ResponseEntity.ok(userReportResultConverter.toResponse(result));
    }
}
