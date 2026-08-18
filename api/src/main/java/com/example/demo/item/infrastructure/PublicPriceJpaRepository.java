package com.example.demo.item.infrastructure;

import com.example.demo.item.domain.PublicPrice;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PublicPriceJpaRepository extends JpaRepository<PublicPrice, Long> {

    List<PublicPrice> findAllByItemIdInAndRegionIdOrderByItemIdAscPriceDateDescIdDesc(
            List<Long> itemIds, String regionId);

    List<PublicPrice> findTop2ByItemIdAndRegionIdOrderByPriceDateDescIdDesc(
            Long itemId, String regionId);

    Optional<PublicPrice> findFirstByItemIdAndRegionIdOrderByPriceDateDescIdDesc(
            Long itemId, String regionId);

    Optional<PublicPrice> findFirstByRegionIdOrderByPriceDateDescIdDesc(String regionId);

    List<PublicPrice>
            findAllByItemIdAndRegionIdAndPriceDateGreaterThanAndPriceDateLessThanEqualOrderByPriceDateAscIdAsc(
                    Long itemId, String regionId, LocalDate startExclusive, LocalDate endInclusive);
}
