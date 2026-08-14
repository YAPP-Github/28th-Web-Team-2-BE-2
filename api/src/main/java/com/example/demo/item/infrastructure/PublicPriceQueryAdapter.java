package com.example.demo.item.infrastructure;

import com.example.demo.item.application.port.PublicPriceQueryPort;
import com.example.demo.item.domain.PublicPrice;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PublicPriceQueryAdapter implements PublicPriceQueryPort {

    private final PublicPriceJpaRepository publicPriceJpaRepository;

    @Override
    public List<PublicPrice> findByItemIdsAndRegionIdAndPriceDate(
            final List<Long> itemIds, final String regionId, final LocalDate priceDate) {
        return publicPriceJpaRepository.findAllByItemIdInAndRegionIdAndPriceDateOrderByItemIdAscIdDesc(
                itemIds, regionId, priceDate);
    }

    @Override
    public LocalDate findLatestPriceDateByRegionId(final String regionId) {
        return publicPriceJpaRepository.findFirstByRegionIdOrderByPriceDateDescIdDesc(regionId)
                .map(PublicPrice::priceDate)
                .orElse(null);
    }
}
