package com.example.demo.item.application.contract;

import java.util.Objects;

public record BatchItemFailure(
        Long itemId,
        Integer channelId,
        int attemptCount,
        RuntimeException cause) {

    public BatchItemFailure(
            final Long itemId,
            final Integer channelId,
            final RuntimeException cause) {
        this(itemId, channelId, 1, cause);
    }

    public BatchItemFailure {
        Objects.requireNonNull(itemId, "itemId must not be null");
        Objects.requireNonNull(channelId, "channelId must not be null");
        Objects.requireNonNull(cause, "cause must not be null");
        if (itemId <= 0 || channelId <= 0) {
            throw new IllegalArgumentException("batch item failure identifiers must be positive");
        }
        if (attemptCount <= 0) {
            throw new IllegalArgumentException("attemptCount must be positive");
        }
    }
}
