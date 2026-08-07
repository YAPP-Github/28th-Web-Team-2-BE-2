package com.example.demo.price.application.port;

import com.example.demo.price.domain.ChannelCode;
import com.example.demo.price.domain.NormalizedPrice;
import java.time.LocalDate;

public interface OnlinePriceRepository {

    void upsert(DailyProductPrice price);

    record DailyProductPrice(
            Long itemId,
            ChannelCode channel,
            String itemName,
            String productName,
            String productUrl,
            NormalizedPrice price,
            LocalDate createdAt) {}
}
