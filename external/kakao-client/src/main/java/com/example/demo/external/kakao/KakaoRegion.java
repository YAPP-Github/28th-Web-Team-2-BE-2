package com.example.demo.external.kakao;

public record KakaoRegion(
        String regionType,
        Long code,
        String region2DepthName,
        String region3DepthName) {}
