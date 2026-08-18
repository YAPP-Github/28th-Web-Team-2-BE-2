package com.example.demo.item.application.port;

import com.example.demo.item.application.query.ItemPublicPriceQuery;
import com.example.demo.item.domain.PublicPrice;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PublicPriceQueryPort {

    List<PublicPrice> findByItemIdsAndRegionId(List<Long> itemIds, String regionId);

    List<PublicPrice> findByItemIdAndRegionId(Long itemId, String regionId);

    Optional<PublicPrice> findLatestByItemIdAndRegionId(Long itemId, String regionId);

    LocalDate findLatestPriceDateByRegionId(String regionId);

    /** {@code (period.startExclusive(baseDate), baseDate]} 구간의 가격을 날짜 오름차순으로 반환한다. */
    List<PublicPrice> findByPeriod(ItemPublicPriceQuery query, LocalDate baseDate);
}
