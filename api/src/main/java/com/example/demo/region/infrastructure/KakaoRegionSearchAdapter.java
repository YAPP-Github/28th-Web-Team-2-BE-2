package com.example.demo.region.infrastructure;

import com.example.demo.external.kakao.KakaoAddressSearchResult;
import com.example.demo.external.kakao.KakaoAddressSearchResult.Address;
import com.example.demo.external.kakao.feign.KakaoMapClient;
import com.example.demo.region.application.port.RegionSearchQueryPort;
import com.example.demo.region.application.query.RegionSearchQuery;
import com.example.demo.region.application.result.RegionSearchResult;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KakaoRegionSearchAdapter implements RegionSearchQueryPort {

    private static final int SEARCH_SIZE = 30;
    private final KakaoMapClient kakaoMapClient;

    @Override
    public RegionSearchResult search(final RegionSearchQuery query) {
        final KakaoAddressSearchResult result = kakaoMapClient.searchAddress(query.keyword(), SEARCH_SIZE);
        final Map<String, RegionSearchResult.Region> regions = new LinkedHashMap<>();
        result.addresses().forEach(address -> addRegion(regions, address.address()));
        return new RegionSearchResult(regions.values().stream().toList());
    }

    private void addRegion(final Map<String, RegionSearchResult.Region> regions, final Address address) {
        final String bCode = address.bCode();
        if (bCode.isBlank()) {
            return;
        }
        regions.putIfAbsent(bCode, new RegionSearchResult.Region(
                bCode, address.region1DepthName() + " "
                        + address.region2DepthName() + " " + address.region3DepthName()));
    }
}
