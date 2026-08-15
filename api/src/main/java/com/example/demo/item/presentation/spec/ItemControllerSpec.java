package com.example.demo.item.presentation.spec;

import com.example.demo.item.presentation.dto.ItemPageResponse;
import com.example.demo.item.presentation.dto.ItemQueryRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;

public interface ItemControllerSpec {

    @Operation(summary = "품목 목록과 공공가격을 조회한다")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "품목 목록 조회 성공",
                content =
                        @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = ItemPageResponse.class))),
        @ApiResponse(responseCode = "400", description = "페이지 요청값이 올바르지 않다")
    })
    ResponseEntity<ItemPageResponse> getItems(@ParameterObject ItemQueryRequest request);
}
