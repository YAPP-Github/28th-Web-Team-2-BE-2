package com.example.demo.report.presentation.spec;

import com.example.demo.report.presentation.dto.RegionItemReportRequest;
import com.example.demo.report.presentation.dto.RegionItemReportResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;

public interface RegionItemReportControllerSpec {

    @Operation(summary = "동네와 품목의 가격 제보 목록을 조회한다")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "제보 목록 조회 성공. 제보가 없으면 빈 목록이다",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = RegionItemReportResponse.class))),
        @ApiResponse(responseCode = "400", description = "법정동 코드 형식이나 조회 조건이 올바르지 않다"),
        @ApiResponse(responseCode = "404", description = "지역 또는 품목을 찾을 수 없다")
    })
    ResponseEntity<RegionItemReportResponse> getRegionItemReports(
            @Pattern(regexp = "\\d{10}") @Parameter(description = "법정동 코드(10자리)") String regionId,
            @Positive @Parameter(description = "품목 ID") Long itemId,
            @Valid @ParameterObject RegionItemReportRequest request);
}
