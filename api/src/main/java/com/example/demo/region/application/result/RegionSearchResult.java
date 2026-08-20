package com.example.demo.region.application.result;

import java.math.BigDecimal;
import java.util.List;

public record RegionSearchResult(List<Region> regions) {

    public record Region(String regionId, String regionName, BigDecimal latitude, BigDecimal longitude) {}
}
