package com.example.demo.item.application.contract;

import java.util.Objects;

public record BatchItemFailure(
        Long itemId,
        Integer channelId,
        RuntimeException cause) {

    private static final int ATTEMPT_COUNT = 1;

    public BatchItemFailure {
        Objects.requireNonNull(itemId, "itemId must not be null");
        Objects.requireNonNull(channelId, "channelId must not be null");
        Objects.requireNonNull(cause, "cause must not be null");
        if (itemId <= 0 || channelId <= 0) {
            throw new IllegalArgumentException("batch item failure identifiers must be positive");
        }
    }

    public int attemptCount() {
        return ATTEMPT_COUNT;
    }
}
