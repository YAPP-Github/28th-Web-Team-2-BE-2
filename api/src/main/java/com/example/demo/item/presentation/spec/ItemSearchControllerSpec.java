package com.example.demo.item.presentation.spec;

import com.example.demo.item.presentation.dto.ItemSearchRequest;
import com.example.demo.item.presentation.dto.ItemSearchResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;

public interface ItemSearchControllerSpec {

    @Operation(summary = "품목명으로 품목을 검색한다")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "품목 검색 성공. 일치하는 품목이 없으면 빈 목록이다",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = ItemSearchResponse.class))),
        @ApiResponse(responseCode = "400", description = "검색 조건이 올바르지 않다")
    })
    ResponseEntity<ItemSearchResponse> searchItems(@Valid @ParameterObject ItemSearchRequest request);
}
