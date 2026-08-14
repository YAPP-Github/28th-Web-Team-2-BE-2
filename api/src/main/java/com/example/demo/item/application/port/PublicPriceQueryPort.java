package com.example.demo.item.application.port;

import com.example.demo.item.domain.PublicPrice;
import java.time.LocalDate;
import java.util.List;

public interface PublicPriceQueryPort {

    List<PublicPrice> findByItemIdsAndRegionIdAndPriceDate(
            List<Long> itemIds, String regionId, LocalDate priceDate);

    LocalDate findLatestPriceDateByRegionId(String regionId);
}
