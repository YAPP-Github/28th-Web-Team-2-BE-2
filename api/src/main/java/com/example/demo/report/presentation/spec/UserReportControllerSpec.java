package com.example.demo.report.presentation.spec;

import com.example.demo.common.security.AuthPrincipal;
import com.example.demo.report.presentation.dto.CreateUserReportRequest;
import com.example.demo.report.presentation.dto.CreateUserReportResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;

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
        @ApiResponse(responseCode = "404", description = "품목을 찾을 수 없다")
    })
    ResponseEntity<CreateUserReportResponse> createReport(
            @Parameter(description = "품목 ID") Long itemId,
            @Valid CreateUserReportRequest request,
            @Parameter(hidden = true) AuthPrincipal principal);
}
