package com.example.demo.kamis.presentation.dto;

import java.util.List;

public record KamisDailyPriceResponse(
        String errorCode,
        String errorMessage,
        List<KamisDailyPriceItemResponse> items) {}
