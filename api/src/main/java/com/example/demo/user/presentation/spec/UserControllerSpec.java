package com.example.demo.user.presentation.spec;

import com.example.demo.common.security.AuthPrincipal;
import com.example.demo.user.presentation.dto.UpdateUserNicknameRequest;
import com.example.demo.user.presentation.dto.UserMeResponse;
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

@Tag(name = "User", description = "사용자 API")
public interface UserControllerSpec {

    @Operation(
            summary = "현재 사용자의 기본 정보를 조회한다",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "사용자 기본 정보 조회 성공",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = UserMeResponse.class))),
        @ApiResponse(responseCode = "401", description = "로그인이 필요하다"),
        @ApiResponse(responseCode = "403", description = "사용자 권한이 필요하다"),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없다")
    })
    ResponseEntity<UserMeResponse> getMe(@Parameter(hidden = true) AuthPrincipal principal);

    @Operation(
            summary = "현재 사용자의 닉네임을 저장한다",
            security = @SecurityRequirement(name = "bearerAuth"),
            requestBody = @RequestBody(
                    required = true,
                    content = @Content(schema = @Schema(implementation = UpdateUserNicknameRequest.class))))
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "닉네임 저장 성공"),
        @ApiResponse(responseCode = "400", description = "닉네임 형식이 올바르지 않다"),
        @ApiResponse(responseCode = "401", description = "로그인이 필요하다"),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없다"),
        @ApiResponse(responseCode = "409", description = "이미 사용 중인 닉네임이다")
    })
    ResponseEntity<Void> updateNickname(
            UpdateUserNicknameRequest request,
            @Parameter(hidden = true) AuthPrincipal principal);
}
