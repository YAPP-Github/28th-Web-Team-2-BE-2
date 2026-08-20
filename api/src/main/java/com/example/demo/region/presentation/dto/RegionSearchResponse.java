package com.example.demo.region.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

public record RegionSearchResponse(List<SearchResult> searchResults) {

    public record SearchResult(
            @Schema(description = "법정동 코드", example = "0111010100") String regionId,
            @Schema(description = "법정동 이름", example = "서울특별시 종로구 청운동") String regionName,
            @Schema(description = "선택 지역 위도", example = "37.5874") BigDecimal latitude,
            @Schema(description = "선택 지역 경도", example = "126.9707") BigDecimal longitude) {}
}
