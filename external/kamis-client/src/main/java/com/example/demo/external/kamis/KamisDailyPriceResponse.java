package com.example.demo.external.kamis;

import java.util.List;

public record KamisDailyPriceResponse(
        String errorCode, String errorMessage, List<KamisDailyPriceItem> items) {}
