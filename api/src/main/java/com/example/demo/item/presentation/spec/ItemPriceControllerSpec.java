package com.example.demo.item.presentation.spec;

import com.example.demo.item.presentation.dto.ItemOnlinePriceResponse;
import com.example.demo.item.presentation.dto.ItemPublicPriceRequest;
import com.example.demo.item.presentation.dto.ItemPublicPriceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;

public interface ItemPriceControllerSpec {

    @Operation(summary = "품목의 기간별 공공가격 추이를 조회한다")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "공공가격 추이 조회 성공. 기간 내 가격이 없으면 빈 목록이다",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = ItemPublicPriceResponse.class))),
        @ApiResponse(responseCode = "400", description = "조회 조건이 올바르지 않다"),
        @ApiResponse(responseCode = "404", description = "품목을 찾을 수 없다")
    })
    ResponseEntity<ItemPublicPriceResponse> getPublicPrices(
            @Positive @Parameter(description = "품목 ID") Long itemId,
            @Valid @ParameterObject ItemPublicPriceRequest request);

    @Operation(summary = "품목의 채널별 온라인 최저가를 조회한다")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "온라인 최저가 조회 성공. 수집 데이터가 없으면 빈 목록이다",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = ItemOnlinePriceResponse.class))),
        @ApiResponse(responseCode = "404", description = "품목을 찾을 수 없다")
    })
    ResponseEntity<ItemOnlinePriceResponse> getOnlinePrices(
            @Positive @Parameter(description = "품목 ID") Long itemId);
}
