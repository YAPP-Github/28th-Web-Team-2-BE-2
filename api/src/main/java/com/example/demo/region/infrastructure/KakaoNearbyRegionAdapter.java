package com.example.demo.region.infrastructure;

import com.example.demo.external.kakao.KakaoMapClient;
import com.example.demo.external.kakao.KakaoRegion;
import com.example.demo.external.kakao.KakaoRegionCodeResult;
import com.example.demo.region.application.port.NearbyRegionQueryPort;
import com.example.demo.region.application.query.NearbyRegionQuery;
import com.example.demo.region.application.result.NearbyRegionResult;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KakaoNearbyRegionAdapter implements NearbyRegionQueryPort {

    private final KakaoMapClient kakaoMapClient;

    @Override
    public NearbyRegionResult find(final NearbyRegionQuery query) {
        final KakaoRegionCodeResult result = kakaoMapClient.searchRegionCode(
                query.longitude(), query.latitude());
        return toResult(result);
    }

    private NearbyRegionResult toResult(final KakaoRegionCodeResult result) {
        final List<NearbyRegionResult.Region> regions = result.legalRegions().stream()
                .map(this::toRegion)
                .toList();
        return new NearbyRegionResult(regions);
    }

    private NearbyRegionResult.Region toRegion(final KakaoRegion region) {
        return new NearbyRegionResult.Region(
                region.code(), region.region2DepthName() + " " + region.region3DepthName());
    }
}
