package com.example.demo.region.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record NearbyRegionResponse(
        @Schema(description = "법정동 코드", example = "4413310500") Long regionId,
        @Schema(description = "법정동 이름", example = "천안시 서북구 성성동") String regionName) {}
