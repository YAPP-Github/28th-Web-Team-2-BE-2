package com.example.demo.region.infrastructure;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import com.example.demo.external.kakao.KakaoClientException;
import com.example.demo.external.kakao.KakaoLocalClient;
import com.example.demo.external.kakao.KakaoRegion;
import com.example.demo.external.kakao.KakaoRegionCodeResult;
import com.example.demo.region.application.port.NearbyRegionQueryPort;
import com.example.demo.region.application.query.NearbyRegionQuery;
import com.example.demo.region.application.result.NearbyRegionResult;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KakaoNearbyRegionAdapter implements NearbyRegionQueryPort {

    private final KakaoLocalClient kakaoLocalClient;

    @Override
    public NearbyRegionResult find(final NearbyRegionQuery query) {
        try {
            final KakaoRegionCodeResult result = kakaoLocalClient.searchRegionCode(
                    query.longitude(), query.latitude());
            return toResult(result);
        } catch (final KakaoClientException exception) {
            throw externalApiException(exception);
        }
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

    private ApiException externalApiException(final KakaoClientException exception) {
        return new ApiException(
                ErrorType.EXTERNAL_API_ERROR.description(),
                ErrorType.EXTERNAL_API_ERROR,
                HttpStatus.BAD_GATEWAY,
                exception);
    }
}
