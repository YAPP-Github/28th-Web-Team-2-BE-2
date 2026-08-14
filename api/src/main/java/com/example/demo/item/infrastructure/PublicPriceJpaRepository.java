package com.example.demo.item.infrastructure;

import com.example.demo.item.domain.PublicPrice;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PublicPriceJpaRepository extends JpaRepository<PublicPrice, Long> {

    Optional<PublicPrice> findFirstByItemIdAndPriceDateOrderByIdDesc(Long itemId, LocalDate priceDate);

    Optional<PublicPrice> findFirstByOrderByPriceDateDescIdDesc();
}
