package com.example.demo.item.application.port;

import com.example.demo.item.domain.OnlinePrice;
import java.time.LocalDate;
import java.util.List;

public interface OnlinePricePersistencePort {

    void deleteAll(Long itemId, Integer channelId, LocalDate collectionDate);

    void saveAll(List<OnlinePrice> prices);
}
