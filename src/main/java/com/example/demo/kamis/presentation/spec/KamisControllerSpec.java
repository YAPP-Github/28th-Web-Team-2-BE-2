package com.example.demo.kamis.presentation.spec;

import com.example.demo.kamis.presentation.dto.KamisDailyPriceRequest;
import com.example.demo.kamis.presentation.dto.KamisDailyPriceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "KAMIS", description = "농수축산물 가격 API")
public interface KamisControllerSpec {

    @Operation(
            summary = "KAMIS 일별 부류별 가격을 조회한다",
            description = "KAMIS의 일별 부류별 도·소매가격을 조회한다.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "가격 조회 성공",
                content = @Content(schema = @Schema(implementation = KamisDailyPriceResponse.class))),
        @ApiResponse(responseCode = "400", description = "조회 조건이 올바르지 않다")
    })
    ResponseEntity<KamisDailyPriceResponse> getDailyPrices(KamisDailyPriceRequest request);
}
