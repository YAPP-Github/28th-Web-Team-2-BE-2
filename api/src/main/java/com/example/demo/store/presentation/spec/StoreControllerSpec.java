package com.example.demo.store.presentation.spec;

import com.example.demo.store.presentation.dto.NearbyStoreRequest;
import com.example.demo.store.presentation.dto.NearbyStoresResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springdoc.core.annotations.ParameterObject;

public interface StoreControllerSpec {

    @Operation(summary = "지도 중심 주변의 가게를 조회한다")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "주변 가게 조회 성공"),
        @ApiResponse(responseCode = "400", description = "조회 조건이 올바르지 않다"),
        @ApiResponse(responseCode = "401", description = "찜 필터는 로그인이 필요하다")
    })
    ResponseEntity<NearbyStoresResponse> getNearbyStores(@ParameterObject NearbyStoreRequest request);
}
