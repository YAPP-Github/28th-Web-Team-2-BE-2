package com.example.demo.region.application.usecase;

import com.example.demo.region.application.port.NearbyRegionQueryPort;
import com.example.demo.region.application.query.NearbyRegionQuery;
import com.example.demo.region.application.result.NearbyRegionResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetNearbyRegionUseCase {

    private final NearbyRegionQueryPort nearbyRegionQueryPort;

    public NearbyRegionResult execute(final NearbyRegionQuery query) {
        return nearbyRegionQueryPort.find(query);
    }
}
