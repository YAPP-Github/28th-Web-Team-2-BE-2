package com.example.demo.region.presentation.converter;

import com.example.demo.region.application.query.NearbyRegionQuery;
import com.example.demo.region.presentation.dto.NearbyRegionRequest;
import org.springframework.stereotype.Component;

@Component
public class RegionQueryConverter {

    public NearbyRegionQuery toNearbyRegionQuery(final NearbyRegionRequest request) {
        return new NearbyRegionQuery(request.latitude(), request.longitude());
    }
}
