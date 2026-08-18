package com.example.demo.item.application.port;

import com.example.demo.item.domain.OnlinePrice;
import java.time.LocalDate;
import java.util.Optional;

public interface OnlinePriceQueryPort {

    Optional<OnlinePrice> findLowestPrice(
            Long itemId, Integer channelId, LocalDate collectionDate, Integer unit);

    Optional<OnlinePrice> findLowestPriceAtLatestCollectionDate(Long itemId, Integer unit);
}
