package com.example.demo.price.application.result;

import com.example.demo.price.domain.CollectionStatus;

public record CollectionResult(CollectionStatus status, int validOfferCount, String failureReason) {}
