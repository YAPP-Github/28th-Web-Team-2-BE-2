package com.example.demo.kamis.application.result;

import java.util.List;

public record KamisDailyPriceResult(
        String errorCode,
        String errorMessage,
        List<KamisDailyPriceItemResult> items) {}
