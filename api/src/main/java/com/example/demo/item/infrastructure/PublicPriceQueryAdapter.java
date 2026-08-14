package com.example.demo.item.infrastructure;

import com.example.demo.item.application.port.PublicPriceQueryPort;
import com.example.demo.item.domain.PublicPrice;
import java.time.LocalDate;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PublicPriceQueryAdapter implements PublicPriceQueryPort {

    private final PublicPriceJpaRepository publicPriceJpaRepository;

    @Override
    public Optional<PublicPrice> findByItemIdAndPriceDate(final Long itemId, final LocalDate priceDate) {
        return publicPriceJpaRepository.findFirstByItemIdAndPriceDateOrderByIdDesc(itemId, priceDate);
    }

    @Override
    public Optional<PublicPrice> findLatest() {
        return publicPriceJpaRepository.findFirstByOrderByPriceDateDescIdDesc();
    }
}
