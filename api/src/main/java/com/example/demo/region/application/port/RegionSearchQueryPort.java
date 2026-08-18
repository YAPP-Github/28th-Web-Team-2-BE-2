package com.example.demo.region.application.port;

import com.example.demo.region.application.query.RegionSearchQuery;
import com.example.demo.region.application.result.RegionSearchResult;

public interface RegionSearchQueryPort {

    RegionSearchResult search(RegionSearchQuery query);
}
