package com.example.demo.item.infrastructure;

import com.example.demo.item.application.port.PublicPriceQueryPort;
import com.example.demo.item.domain.PublicPrice;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PublicPriceQueryAdapter implements PublicPriceQueryPort {

    private final PublicPriceJpaRepository publicPriceJpaRepository;

    @Override
    public List<PublicPrice> findByItemIdsAndRegionId(
            final List<Long> itemIds, final String regionId) {
        return publicPriceJpaRepository.findAllByItemIdInAndRegionIdOrderByItemIdAscPriceDateDescIdDesc(
                itemIds, regionId);
    }

    @Override
    public List<PublicPrice> findByItemIdAndRegionId(
            final Long itemId, final String regionId) {
        return publicPriceJpaRepository.findAllByItemIdAndRegionIdOrderByPriceDateDescIdDesc(
                itemId, regionId);
    }

    @Override
    public Optional<PublicPrice> findLatestByItemIdAndRegionId(
            final Long itemId, final String regionId) {
        return publicPriceJpaRepository.findFirstByItemIdAndRegionIdOrderByPriceDateDescIdDesc(
                itemId, regionId);
    }

    @Override
    public LocalDate findLatestPriceDateByRegionId(final String regionId) {
        return publicPriceJpaRepository.findFirstByRegionIdOrderByPriceDateDescIdDesc(regionId)
                .map(PublicPrice::priceDate)
                .orElse(null);
    }
}
