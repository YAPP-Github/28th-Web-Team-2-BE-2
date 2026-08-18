package com.example.demo.item.application.port;

import com.example.demo.item.domain.PublicPrice;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PublicPriceQueryPort {

    List<PublicPrice> findByItemIdsAndRegionId(List<Long> itemIds, String regionId);

    Optional<PublicPrice> findLatestByItemIdAndRegionId(Long itemId, String regionId);

    LocalDate findLatestPriceDateByRegionId(String regionId);
}
