package com.example.demo.kamis.application.port;

import com.example.demo.kamis.application.query.KamisDailyPriceQuery;
import com.example.demo.kamis.application.result.KamisDailyPriceResult;

public interface KamisPriceQueryPort {

    KamisDailyPriceResult findDailyPrices(KamisDailyPriceQuery query);
}
