package com.example.demo.price.application.command;

import com.example.demo.price.domain.ChannelCode;
import com.example.demo.price.domain.PriceUnit;
import java.time.LocalDate;

public record CollectionTask(
        Long itemId,
        String itemName,
        ChannelCode channel,
        PriceUnit targetUnit,
        LocalDate priceDate,
        Long executionId) {

    public CollectionTask {
        if (itemId == null || itemId <= 0) {
            throw new IllegalArgumentException("item id must be positive");
        }
        if (itemName == null || itemName.isBlank()) {
            throw new IllegalArgumentException("item name must not be blank");
        }
        if (channel == null || targetUnit == null || priceDate == null || executionId == null) {
            throw new IllegalArgumentException("collection task fields must not be null");
        }
    }
}
