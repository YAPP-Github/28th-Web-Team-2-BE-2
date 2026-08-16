package com.example.demo.region.application.port;

import com.example.demo.region.application.query.NearbyRegionQuery;
import com.example.demo.region.application.result.NearbyRegionResult;

public interface NearbyRegionQueryPort {

    NearbyRegionResult find(NearbyRegionQuery query);
}
