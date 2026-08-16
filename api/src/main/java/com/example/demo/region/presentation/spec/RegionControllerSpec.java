package com.example.demo.region.presentation.spec;

import com.example.demo.region.presentation.dto.NearbyRegionRequest;
import com.example.demo.region.presentation.dto.NearbyRegionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;

@Tag(name = "REGION", description = "지역 API")
public interface RegionControllerSpec {

    @Operation(summary = "좌표에 해당하는 법정동을 조회한다")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "법정동 조회 성공",
                content = @Content(array = @ArraySchema(schema = @Schema(implementation = NearbyRegionResponse.class)))),
        @ApiResponse(responseCode = "400", description = "좌표가 올바르지 않다")
    })
    ResponseEntity<List<NearbyRegionResponse>> getNearbyRegions(NearbyRegionRequest request);
}
