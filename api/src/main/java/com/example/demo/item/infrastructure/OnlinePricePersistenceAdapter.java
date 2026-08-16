package com.example.demo.item.infrastructure;

import com.example.demo.item.application.port.OnlinePricePersistencePort;
import com.example.demo.item.application.port.OnlinePriceQueryPort;
import com.example.demo.item.domain.OnlinePrice;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class OnlinePricePersistenceAdapter implements OnlinePricePersistencePort, OnlinePriceQueryPort {

    private final OnlinePriceJpaRepository onlinePriceJpaRepository;

    @Override
    public void deleteAll(final Long itemId, final Integer channelId, final LocalDate collectionDate) {
        onlinePriceJpaRepository.deleteAllByItemIdAndChannelIdAndCreatedAt(
                itemId, channelId, collectionDate);
    }

    @Override
    public void saveAll(final List<OnlinePrice> prices) {
        onlinePriceJpaRepository.saveAll(prices);
    }

    @Override
    public Optional<OnlinePrice> findLowestPrice(
            final Long itemId,
            final Integer channelId,
            final LocalDate collectionDate,
            final Integer unit) {
        return onlinePriceJpaRepository
                .findFirstByItemIdAndChannelIdAndCreatedAtAndUnitOrderByPriceAscIdAsc(
                        itemId, channelId, collectionDate, unit);
    }
}
