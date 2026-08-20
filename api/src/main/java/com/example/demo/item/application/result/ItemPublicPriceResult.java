package com.example.demo.item.application.result;

import com.example.demo.item.application.query.PublicPricePeriod;
import java.time.LocalDate;
import java.util.List;

public record ItemPublicPriceResult(
        Long itemId, String defaultUnit, PublicPricePeriod period, List<Point> points) {

    public record Point(LocalDate date, Integer price) {}
}
