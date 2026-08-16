package com.example.demo.item.application.usecase;

import com.example.demo.item.application.port.OnlinePricePersistencePort;
import com.example.demo.item.domain.OnlinePrice;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReplaceOnlinePriceUseCase {

    private final OnlinePricePersistencePort onlinePricePersistencePort;

    @Transactional
    public void execute(
            final Long itemId,
            final Integer channelId,
            final LocalDate collectionDate,
            final List<OnlinePrice> prices) {
        onlinePricePersistencePort.deleteAll(itemId, channelId, collectionDate);
        if (prices.isEmpty()) {
            return;
        }
        onlinePricePersistencePort.saveAll(prices);
    }
}
