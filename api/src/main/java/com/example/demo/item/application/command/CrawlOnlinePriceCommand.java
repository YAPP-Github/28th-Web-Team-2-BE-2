package com.example.demo.item.application.command;

import com.example.demo.common.exception.ApiException;

public record CrawlOnlinePriceCommand(String itemName) {

    public CrawlOnlinePriceCommand {
        if (itemName == null || itemName.isBlank()) {
            throw ApiException.invalidParameter();
        }
    }
}
