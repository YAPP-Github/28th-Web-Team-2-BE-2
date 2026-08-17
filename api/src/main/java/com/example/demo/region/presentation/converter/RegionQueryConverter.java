package com.example.demo.region.presentation.converter;

import com.example.demo.region.application.query.NearbyRegionQuery;
import com.example.demo.region.application.query.RegionSearchQuery;
import com.example.demo.region.presentation.dto.NearbyRegionRequest;
import com.example.demo.region.presentation.dto.RegionSearchRequest;
import org.springframework.stereotype.Component;

@Component
public class RegionQueryConverter {

    public NearbyRegionQuery toNearbyRegionQuery(final NearbyRegionRequest request) {
        return new NearbyRegionQuery(request.latitude(), request.longitude());
    }

    public RegionSearchQuery toRegionSearchQuery(final RegionSearchRequest request) {
        return new RegionSearchQuery(request.keyword());
    }
}
