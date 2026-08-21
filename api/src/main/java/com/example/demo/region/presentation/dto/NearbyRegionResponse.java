package com.example.demo.region.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

public record NearbyRegionResponse(
        @Schema(description = "법정동 코드", example = "4413310500") String regionId,
        @Schema(description = "법정동 이름", example = "천안시 서북구 성성동") String regionName,
        @Schema(description = "조회 요청에 사용한 위도", example = "36.8358") BigDecimal latitude,
        @Schema(description = "조회 요청에 사용한 경도", example = "127.1324") BigDecimal longitude) {}
