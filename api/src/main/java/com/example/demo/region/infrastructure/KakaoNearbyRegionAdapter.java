package com.example.demo.region.infrastructure;

import com.example.demo.external.kakao.KakaoRegion;
import com.example.demo.external.kakao.KakaoRegionCodeResult;
import com.example.demo.external.kakao.feign.KakaoMapClient;
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
        return toResult(result, query);
    }

    private NearbyRegionResult toResult(
            final KakaoRegionCodeResult result, final NearbyRegionQuery query) {
        final List<NearbyRegionResult.Region> regions = result.legalRegions().stream()
                .map(region -> toRegion(region, query))
                .toList();
        return new NearbyRegionResult(regions);
    }

    private NearbyRegionResult.Region toRegion(
            final KakaoRegion region, final NearbyRegionQuery query) {
        return new NearbyRegionResult.Region(
                region.code(),
                region.region2DepthName() + " " + region.region3DepthName(),
                query.latitude(),
                query.longitude());
    }
}
