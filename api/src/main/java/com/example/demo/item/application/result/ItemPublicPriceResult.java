package com.example.demo.item.application.result;

import com.example.demo.item.application.query.PublicPricePeriod;
import java.util.List;

public record ItemPublicPriceResult(
        Long itemId, String defaultUnit, PublicPricePeriod period, List<PublicPricePointResult> points) {}
