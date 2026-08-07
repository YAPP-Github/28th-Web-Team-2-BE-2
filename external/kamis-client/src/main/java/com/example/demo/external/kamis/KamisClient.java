package com.example.demo.external.kamis;

public interface KamisClient {

    KamisDailyPriceResponse getDailyPrices(KamisDailyPriceRequest request);
}
