package com.example.demo.external.kakao;

import java.util.List;

public record KakaoCategorySearchResult(
        long totalCount,
        long pageableCount,
        boolean end,
        List<KakaoPlace> places) {}
