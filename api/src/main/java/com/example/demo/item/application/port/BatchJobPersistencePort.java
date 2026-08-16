package com.example.demo.item.application.port;

import com.example.demo.item.application.result.BatchJobStatus;

public interface BatchJobPersistencePort {

    Long start(String jobName);

    void recordItemError(
            Long jobExecutionId,
            Long itemId,
            Integer channelId,
            int attemptCount,
            String errorType,
            String errorMessage);

    void finish(
            Long jobExecutionId,
            BatchJobStatus status,
            int totalRecords,
            int successRecords,
            String errorMessage);
}
