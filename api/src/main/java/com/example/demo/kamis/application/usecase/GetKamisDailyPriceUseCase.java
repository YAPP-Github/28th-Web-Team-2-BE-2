package com.example.demo.kamis.application.usecase;

import com.example.demo.kamis.application.port.KamisPriceQueryPort;
import com.example.demo.kamis.application.query.KamisDailyPriceQuery;
import com.example.demo.kamis.application.result.KamisDailyPriceResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetKamisDailyPriceUseCase {

    private final KamisPriceQueryPort kamisPriceQueryPort;

    public KamisDailyPriceResult execute(final KamisDailyPriceQuery query) {
        return kamisPriceQueryPort.findDailyPrices(query);
    }
}
