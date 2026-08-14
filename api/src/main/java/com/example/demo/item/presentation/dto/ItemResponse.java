package com.example.demo.item.presentation.dto;

public record ItemResponse(
        Long itemId,
        String itemName,
        String itemImageUrl,
        Integer price,
        Integer priceGap) {}
