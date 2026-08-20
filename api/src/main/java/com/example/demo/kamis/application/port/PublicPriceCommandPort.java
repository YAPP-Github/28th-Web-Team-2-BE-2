package com.example.demo.kamis.application.port;

import java.time.LocalDate;
import java.util.List;

public interface PublicPriceCommandPort {

    int upsertAll(List<PublicPriceCommand> prices);

    record PublicPriceCommand(Long itemId, String regionId, int price, LocalDate priceDate) {}
}
