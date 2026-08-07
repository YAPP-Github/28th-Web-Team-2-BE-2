package com.example.demo.price.application.port;

import com.example.demo.price.domain.CollectionStatus;
import java.time.OffsetDateTime;

public interface CollectionExecutionRepository {

    void record(TaskExecution execution);

    record TaskExecution(
            Long executionId,
            Long itemId,
            String itemName,
            String channel,
            CollectionStatus status,
            OffsetDateTime startedAt,
            OffsetDateTime finishedAt,
            int validOfferCount,
            String failureReason) {}
}
