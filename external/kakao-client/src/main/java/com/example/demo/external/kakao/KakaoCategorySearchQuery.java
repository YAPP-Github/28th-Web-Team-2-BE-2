package com.example.demo.external.kakao;

import java.math.BigDecimal;

public record KakaoCategorySearchQuery(
        BigDecimal latitude,
        BigDecimal longitude,
        Integer radius) {}
