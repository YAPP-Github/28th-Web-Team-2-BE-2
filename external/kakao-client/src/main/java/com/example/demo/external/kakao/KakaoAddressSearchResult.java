package com.example.demo.external.kakao;

import java.math.BigDecimal;
import java.util.List;

public record KakaoAddressSearchResult(long totalCount, List<KakaoAddress> addresses) {

    public record KakaoAddress(
            String addressName,
            String addressType,
            BigDecimal longitude,
            BigDecimal latitude,
            Address address) {}

    public record Address(
            String addressName,
            String region1DepthName,
            String region2DepthName,
            String region3DepthName,
            String bCode) {}
}
