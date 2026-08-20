package com.example.demo.item.infrastructure;

import com.example.demo.item.domain.PublicPrice;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PublicPriceJpaRepository extends JpaRepository<PublicPrice, Long> {

    List<PublicPrice> findAllByItemIdInAndRegionIdOrderByItemIdAscPriceDateDescIdDesc(
            List<Long> itemIds, String regionId);

    List<PublicPrice> findAllByItemIdAndRegionIdOrderByPriceDateDescIdDesc(Long itemId, String regionId);

    List<PublicPrice> findTop2ByItemIdAndRegionIdOrderByPriceDateDescIdDesc(
            Long itemId, String regionId);

    Optional<PublicPrice> findFirstByItemIdAndRegionIdOrderByPriceDateDescIdDesc(
            Long itemId, String regionId);

    Optional<PublicPrice> findFirstByRegionIdOrderByPriceDateDescIdDesc(String regionId);

    @Query("""
            select price from PublicPrice price
            where price.itemId = :itemId
              and price.regionId = :regionId
              and price.priceDate > :startExclusive
              and price.priceDate <= :endInclusive
            order by price.priceDate asc, price.id asc
            """)
    List<PublicPrice> findByRange(
            Long itemId, String regionId, LocalDate startExclusive, LocalDate endInclusive);
}
