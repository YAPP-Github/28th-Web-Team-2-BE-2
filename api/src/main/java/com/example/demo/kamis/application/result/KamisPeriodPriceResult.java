package com.example.demo.kamis.application.result;

import java.util.List;

public record KamisPeriodPriceResult(
        String errorCode,
        String errorMessage,
        List<KamisPeriodPriceItemResult> items) {}
