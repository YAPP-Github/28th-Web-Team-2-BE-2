package com.example.demo.user.presentation.spec;

import com.example.demo.common.security.AuthPrincipal;
import com.example.demo.user.presentation.dto.AddUserRegionRequest;
import com.example.demo.user.presentation.dto.UserRegionsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "User Region", description = "사용자 관심 지역 API")
public interface UserRegionControllerSpec {

    @Operation(
            summary = "현재 사용자의 관심 지역을 조회한다",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "관심 지역 조회 성공",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = UserRegionsResponse.class))),
        @ApiResponse(responseCode = "401", description = "로그인이 필요하다"),
        @ApiResponse(responseCode = "403", description = "사용자 권한이 필요하다")
    })
    ResponseEntity<UserRegionsResponse> getRegions(@Parameter(hidden = true) AuthPrincipal principal);

    @Operation(
            summary = "현재 사용자의 관심 지역을 추가한다",
            security = @SecurityRequirement(name = "bearerAuth"),
            requestBody = @RequestBody(
                    required = true,
                    content = @Content(schema = @Schema(implementation = AddUserRegionRequest.class))))
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "관심 지역 추가 성공"),
        @ApiResponse(responseCode = "400", description = "법정동 코드가 올바르지 않다"),
        @ApiResponse(responseCode = "401", description = "로그인이 필요하다"),
        @ApiResponse(responseCode = "403", description = "사용자 권한이 필요하다"),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없다"),
        @ApiResponse(responseCode = "409", description = "중복 또는 최대 개수 초과")
    })
    ResponseEntity<Void> addRegion(AddUserRegionRequest request, @Parameter(hidden = true) AuthPrincipal principal);

    @Operation(
            summary = "현재 사용자의 현재 관심 지역을 변경한다",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "현재 관심 지역 변경 성공"),
        @ApiResponse(responseCode = "400", description = "법정동 코드가 올바르지 않다"),
        @ApiResponse(responseCode = "401", description = "로그인이 필요하다"),
        @ApiResponse(responseCode = "403", description = "사용자 권한이 필요하다"),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없다"),
        @ApiResponse(responseCode = "409", description = "관심 지역 최대 개수 초과")
    })
    ResponseEntity<Void> setCurrentRegion(
            @Parameter(description = "외부 법정동 코드", example = "1121510100") String regionId,
            @Parameter(hidden = true) AuthPrincipal principal);
}
