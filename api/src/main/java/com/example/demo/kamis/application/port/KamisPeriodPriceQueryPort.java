package com.example.demo.kamis.application.port;

import com.example.demo.kamis.application.query.KamisPeriodPriceQuery;
import com.example.demo.kamis.application.result.KamisPeriodPriceResult;

public interface KamisPeriodPriceQueryPort {

    KamisPeriodPriceResult findWholesalePeriodPrices(KamisPeriodPriceQuery query);
}
