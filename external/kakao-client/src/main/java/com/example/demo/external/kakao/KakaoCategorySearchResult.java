package com.example.demo.external.kakao;

import java.util.List;

public record KakaoCategorySearchResult(long totalCount, List<KakaoPlace> places) {}
