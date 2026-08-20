package com.example.demo.item.infrastructure;

import com.example.demo.item.application.port.PublicPriceQueryPort;
import com.example.demo.item.application.query.PublicPriceRange;
import com.example.demo.item.domain.PublicPrice;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
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
        return publicPriceJpaRepository.findAllByItemIdAndRegionIdOrderByPriceDateDescIdDesc(itemId, regionId).stream()
                .collect(Collectors.toMap(
                        PublicPrice::priceDate,
                        price -> price,
                        (latest, ignored) -> latest,
                        LinkedHashMap::new))
                .values().stream()
                .limit(2)
                .toList();
    }

    @Override
    public Optional<PublicPrice> findLatestByItemIdAndRegionId(
            final Long itemId, final String regionId) {
        return publicPriceJpaRepository.findFirstByItemIdAndRegionIdOrderByPriceDateDescIdDesc(
                itemId, regionId);
    }

    @Override
    public List<PublicPrice> findByRange(
            final Long itemId, final String regionId, final PublicPriceRange range) {
        return publicPriceJpaRepository.findByRange(
                itemId, regionId, range.startExclusive(), range.endInclusive());
    }

    @Override
    public LocalDate findLatestPriceDateByRegionId(final String regionId) {
        return publicPriceJpaRepository.findFirstByRegionIdOrderByPriceDateDescIdDesc(regionId)
                .map(PublicPrice::priceDate)
                .orElse(null);
    }
}
