package com.example.demo.item.application.result;

public record OnlinePriceCollectionResult(
        int totalTaskCount,
        int succeededTaskCount,
        int failedTaskCount,
        int savedPriceCount) {}
