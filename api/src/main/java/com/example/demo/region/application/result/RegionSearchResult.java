package com.example.demo.region.application.result;

import java.util.List;

public record RegionSearchResult(List<Region> regions) {

    public record Region(String regionId, String regionName) {}
}
