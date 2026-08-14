package com.example.demo.item.application.port;

import com.example.demo.item.domain.PublicPrice;
import java.time.LocalDate;
import java.util.Optional;

public interface PublicPriceQueryPort {

    Optional<PublicPrice> findByItemIdAndPriceDate(Long itemId, LocalDate priceDate);

    Optional<PublicPrice> findLatest();
}
