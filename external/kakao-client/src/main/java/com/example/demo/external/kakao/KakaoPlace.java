package com.example.demo.external.kakao;

import java.math.BigDecimal;

public record KakaoPlace(
        String id,
        String placeName,
        BigDecimal latitude,
        BigDecimal longitude,
        String addressName,
        String roadAddressName,
        String phone,
        String placeUrl,
        Integer distanceMeters) {}
