package com.example.demo.report.presentation.spec;

import com.example.demo.common.security.AuthPrincipal;
import com.example.demo.report.presentation.dto.CreateUserReportRequest;
import com.example.demo.report.presentation.dto.CreateUserReportResponse;
import com.example.demo.report.presentation.dto.StoreReportsRequest;
import com.example.demo.report.presentation.dto.StoreReportsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springdoc.core.annotations.ParameterObject;

public interface UserReportControllerSpec {

    @Operation(summary = "품목 가격을 제보한다", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(
                responseCode = "201",
                description = "가격 제보 성공",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = CreateUserReportResponse.class))),
        @ApiResponse(responseCode = "400", description = "제보 입력값이 올바르지 않다"),
        @ApiResponse(responseCode = "401", description = "로그인이 필요하다"),
        @ApiResponse(responseCode = "404", description = "품목 또는 매장을 찾을 수 없다"),
        @ApiResponse(responseCode = "409", description = "동일한 제보가 이미 존재한다")
    })
    ResponseEntity<CreateUserReportResponse> createReport(
            @Parameter(description = "품목 ID") Long itemId,
            @Valid CreateUserReportRequest request,
            @Parameter(hidden = true) AuthPrincipal principal);

    @Operation(summary = "가게별 가격 제보를 조회한다", description = "filter를 생략하면 CHEAP, ALL이면 전체 제보를 조회한다")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "가게별 가격 제보 조회 성공"),
        @ApiResponse(responseCode = "400", description = "조회 조건이 올바르지 않다"),
        @ApiResponse(responseCode = "404", description = "가게를 찾을 수 없다")
    })
    ResponseEntity<StoreReportsResponse> getStoreReports(
            @Parameter(description = "가게 ID") @Positive Long storeId,
            @Valid @ParameterObject StoreReportsRequest request);
}
