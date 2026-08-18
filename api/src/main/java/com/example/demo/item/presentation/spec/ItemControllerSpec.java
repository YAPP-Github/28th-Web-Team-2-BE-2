package com.example.demo.item.presentation.spec;

import com.example.demo.common.security.AuthPrincipal;
import com.example.demo.item.presentation.dto.ItemPageResponse;
import com.example.demo.item.presentation.dto.ItemDetailRequest;
import com.example.demo.item.presentation.dto.ItemDetailResponse;
import com.example.demo.item.presentation.dto.ItemQueryRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

public interface ItemControllerSpec {

    @Operation(summary = "품목 상세를 조회한다")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "품목 상세 조회 성공",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = ItemDetailResponse.class))),
        @ApiResponse(responseCode = "400", description = "조회 조건이 올바르지 않다"),
        @ApiResponse(responseCode = "404", description = "품목을 찾을 수 없다")
    })
    ResponseEntity<ItemDetailResponse> getItemDetail(
            @Positive @Parameter(description = "품목 ID") Long itemId,
            @Valid @ParameterObject ItemDetailRequest request,
            @Parameter(hidden = true) AuthPrincipal principal,
            @Parameter(hidden = true) Authentication authentication);

    @Operation(summary = "품목 목록과 공공가격을 조회한다")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "품목 목록 조회 성공",
                content =
                        @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = ItemPageResponse.class))),
        @ApiResponse(responseCode = "400", description = "조회 조건이 올바르지 않다"),
        @ApiResponse(responseCode = "401", description = "찜한 품목만 조회하려면 로그인이 필요하다")
    })
    ResponseEntity<ItemPageResponse> getItems(
            @Valid @ParameterObject ItemQueryRequest request,
            @Parameter(hidden = true) AuthPrincipal principal,
            @Parameter(hidden = true) Authentication authentication);

    @Operation(summary = "품목을 찜한다", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "찜 추가 성공"),
        @ApiResponse(responseCode = "401", description = "로그인이 필요하다"),
        @ApiResponse(responseCode = "404", description = "품목을 찾을 수 없다")
    })
    ResponseEntity<Void> addFavorite(
            @Parameter(description = "품목 ID") Long itemId,
            @Parameter(hidden = true) AuthPrincipal principal);

    @Operation(summary = "품목 찜을 해제한다", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "찜 삭제 성공"),
        @ApiResponse(responseCode = "401", description = "로그인이 필요하다"),
        @ApiResponse(responseCode = "404", description = "품목을 찾을 수 없다")
    })
    ResponseEntity<Void> deleteFavorite(
            @Parameter(description = "품목 ID") Long itemId,
            @Parameter(hidden = true) AuthPrincipal principal);
}
