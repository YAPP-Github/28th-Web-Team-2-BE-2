package com.example.demo.item.infrastructure;

import com.example.demo.item.domain.PublicPrice;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PublicPriceJpaRepository extends JpaRepository<PublicPrice, Long> {

    List<PublicPrice> findAllByItemIdInAndRegionIdAndPriceDateOrderByItemIdAscIdDesc(
            List<Long> itemIds, String regionId, LocalDate priceDate);

    Optional<PublicPrice> findFirstByRegionIdOrderByPriceDateDescIdDesc(String regionId);
}
