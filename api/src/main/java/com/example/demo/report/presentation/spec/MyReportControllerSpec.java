package com.example.demo.report.presentation.spec;

import com.example.demo.common.security.AuthPrincipal;
import com.example.demo.report.presentation.dto.MyReportPageResponse;
import com.example.demo.report.presentation.dto.MyReportRequest;
import com.example.demo.report.presentation.dto.MyWeeklyReportResponse;
import com.example.demo.report.presentation.dto.UpdateUserReportRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;

public interface MyReportControllerSpec {

    @Operation(
            summary = "내가 작성한 가격 제보 목록을 조회한다",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "내 제보 목록 조회 성공. 제보가 없으면 빈 목록이다",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = MyReportPageResponse.class))),
        @ApiResponse(responseCode = "400", description = "조회 조건이 올바르지 않다"),
        @ApiResponse(responseCode = "401", description = "로그인이 필요하다. 게스트 토큰도 401이다")
    })
    ResponseEntity<MyReportPageResponse> getMyReports(
            @Valid @ParameterObject MyReportRequest request,
            @Parameter(hidden = true) AuthPrincipal principal);

    @Operation(
            summary = "내 주간 제보 현황을 조회한다",
            description = "Asia/Seoul 기준 이번 주 월요일부터 7일의 제보 여부를 반환한다",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "주간 제보 현황 조회 성공. 제보가 없으면 0건과 false 7일이다",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = MyWeeklyReportResponse.class))),
        @ApiResponse(responseCode = "401", description = "로그인이 필요하다. 게스트 토큰도 401이다")
    })
    ResponseEntity<MyWeeklyReportResponse> getMyWeeklyReports(
            @Parameter(hidden = true) AuthPrincipal principal);

    @Operation(
            summary = "내 가격 제보를 수정한다",
            security = @SecurityRequirement(name = "bearerAuth"),
            requestBody = @RequestBody(
                    required = true,
                    content = @Content(schema = @Schema(implementation = UpdateUserReportRequest.class))))
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "가격 제보 수정 성공"),
        @ApiResponse(responseCode = "400", description = "수정할 가격·수량·단위가 올바르지 않다"),
        @ApiResponse(responseCode = "401", description = "로그인이 필요하다"),
        @ApiResponse(responseCode = "403", description = "사용자 권한이 필요하다"),
        @ApiResponse(responseCode = "404", description = "내 제보를 찾을 수 없다")
    })
    ResponseEntity<Void> updateMyReport(
            @Parameter(description = "제보 ID") @Positive Long reportId,
            @Valid UpdateUserReportRequest request,
            @Parameter(hidden = true) AuthPrincipal principal);

    @Operation(
            summary = "내 가격 제보를 삭제한다",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "가격 제보 삭제 성공"),
        @ApiResponse(responseCode = "401", description = "로그인이 필요하다"),
        @ApiResponse(responseCode = "403", description = "사용자 권한이 필요하다"),
        @ApiResponse(responseCode = "404", description = "내 제보를 찾을 수 없다")
    })
    ResponseEntity<Void> deleteMyReport(
            @Parameter(description = "제보 ID") @Positive Long reportId,
            @Parameter(hidden = true) AuthPrincipal principal);
}
