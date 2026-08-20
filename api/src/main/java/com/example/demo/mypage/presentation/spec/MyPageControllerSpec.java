package com.example.demo.mypage.presentation.spec;

import com.example.demo.common.security.AuthPrincipal;
import com.example.demo.mypage.presentation.dto.FavoriteStoresRequest;
import com.example.demo.mypage.presentation.dto.FavoriteStoresResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springdoc.core.annotations.ParameterObject;

@Tag(name = "My Page", description = "마이페이지 API")
public interface MyPageControllerSpec {

    @Operation(
            summary = "현재 사용자의 단골 가게를 조회한다",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "단골 가게 조회 성공"),
        @ApiResponse(responseCode = "400", description = "페이지 또는 좌표 조건이 올바르지 않다"),
        @ApiResponse(responseCode = "401", description = "로그인이 필요하다"),
        @ApiResponse(responseCode = "403", description = "사용자 권한이 필요하다")
    })
    ResponseEntity<FavoriteStoresResponse> getFavoriteStores(
            @Valid @ParameterObject FavoriteStoresRequest request,
            @Parameter(hidden = true) AuthPrincipal principal);
}
