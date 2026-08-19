package com.example.demo.store.presentation.spec;

import com.example.demo.common.security.AuthPrincipal;
import com.example.demo.store.presentation.dto.NearbyStoreRequest;
import com.example.demo.store.presentation.dto.NearbyStoresResponse;
import com.example.demo.store.presentation.dto.RecommendedStoreRequest;
import com.example.demo.store.presentation.dto.RecommendedStoresResponse;
import com.example.demo.store.presentation.dto.StoreDetailRequest;
import com.example.demo.store.presentation.dto.StoreDetailResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springdoc.core.annotations.ParameterObject;

public interface StoreControllerSpec {

    @Operation(summary = "지도 중심 주변의 가게를 조회한다")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "주변 가게 조회 성공"),
        @ApiResponse(responseCode = "400", description = "조회 조건이 올바르지 않다"),
        @ApiResponse(responseCode = "401", description = "찜 필터는 로그인이 필요하다"),
        @ApiResponse(responseCode = "500", description = "로컬 가게 동기화에 실패했다"),
        @ApiResponse(responseCode = "502", description = "외부 가게 provider를 조회할 수 없다")
    })
    ResponseEntity<NearbyStoresResponse> getNearbyStores(
            @Valid @ParameterObject NearbyStoreRequest request,
            @Parameter(hidden = true) AuthPrincipal principal,
            @Parameter(hidden = true) Authentication authentication,
            @Parameter(hidden = true) HttpServletRequest servletRequest);

    @Operation(summary = "추천 가게를 조회한다")
    @ApiResponse(responseCode = "200", description = "추천 가게 조회 성공")
    ResponseEntity<RecommendedStoresResponse> getRecommendedStores(
            @Valid @ParameterObject RecommendedStoreRequest request,
            @Parameter(hidden = true) HttpServletRequest servletRequest);

    @Operation(summary = "가게 상세를 조회한다")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "가게 상세 조회 성공"),
        @ApiResponse(responseCode = "400", description = "가게 ID 또는 좌표가 올바르지 않다"),
        @ApiResponse(responseCode = "401", description = "인증 토큰이 올바르지 않다"),
        @ApiResponse(responseCode = "404", description = "가게를 찾을 수 없다")
    })
    ResponseEntity<StoreDetailResponse> getStoreDetail(
            @Parameter(description = "가게 ID") @Positive Long storeId,
            @Valid @ParameterObject StoreDetailRequest request,
            @Parameter(hidden = true) AuthPrincipal principal,
            @Parameter(hidden = true) Authentication authentication,
            @Parameter(hidden = true) HttpServletRequest servletRequest);

    @Operation(summary = "가게를 단골로 등록한다")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "단골 등록 성공 또는 이미 등록된 관계"),
        @ApiResponse(responseCode = "401", description = "로그인이 필요하다"),
        @ApiResponse(responseCode = "404", description = "가게를 찾을 수 없다")
    })
    ResponseEntity<Void> addFavorite(
            @Parameter(description = "가게 ID") @Positive Long storeId,
            @Parameter(hidden = true) AuthPrincipal principal);
}
