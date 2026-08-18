package com.example.demo.store.presentation.spec;

import com.example.demo.common.security.AuthPrincipal;
import com.example.demo.store.presentation.dto.NearbyStoreRequest;
import com.example.demo.store.presentation.dto.NearbyStoresResponse;
import com.example.demo.store.presentation.dto.RecommendedStoreRequest;
import com.example.demo.store.presentation.dto.RecommendedStoresResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
            @ParameterObject NearbyStoreRequest request,
            @Parameter(hidden = true) AuthPrincipal principal,
            @Parameter(hidden = true) Authentication authentication,
            @Parameter(hidden = true) HttpServletRequest servletRequest);

    @Operation(summary = "제보 가격이 저렴한 주변 가게를 조회한다")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "추천 가게 조회 성공"),
        @ApiResponse(responseCode = "400", description = "조회 조건이 올바르지 않다")
    })
    ResponseEntity<RecommendedStoresResponse> getRecommendedStores(
            @ParameterObject RecommendedStoreRequest request);
}
