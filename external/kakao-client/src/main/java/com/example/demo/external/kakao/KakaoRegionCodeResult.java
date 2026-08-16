package com.example.demo.external.kakao;

import java.util.List;

public record KakaoRegionCodeResult(long totalCount, List<KakaoRegion> regions) {

    private static final String LEGAL_REGION_TYPE = "B";

    public List<KakaoRegion> legalRegions() {
        return regions.stream()
                .filter(region -> LEGAL_REGION_TYPE.equals(region.regionType()))
                .toList();
    }
}
