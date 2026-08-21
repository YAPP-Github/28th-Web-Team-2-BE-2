package com.example.demo.region.presentation.converter;

import com.example.demo.region.application.result.NearbyRegionResult;
import com.example.demo.region.application.result.RegionSearchResult;
import com.example.demo.region.presentation.dto.NearbyRegionResponse;
import com.example.demo.region.presentation.dto.RegionSearchResponse;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RegionResultConverter {

    public List<NearbyRegionResponse> toNearbyRegionResponses(final NearbyRegionResult result) {
        return result.regions().stream()
                .map(region -> new NearbyRegionResponse(
                        region.regionId(),
                        region.regionName(),
                        region.latitude(),
                        region.longitude()))
                .toList();
    }

    public RegionSearchResponse toRegionSearchResponse(final RegionSearchResult result) {
        return new RegionSearchResponse(result.regions().stream()
                .map(region -> new RegionSearchResponse.SearchResult(
                        region.regionId(), region.regionName(), region.latitude(), region.longitude()))
                .toList());
    }
}
