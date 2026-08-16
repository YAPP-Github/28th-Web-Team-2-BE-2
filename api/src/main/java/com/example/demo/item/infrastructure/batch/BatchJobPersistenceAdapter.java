package com.example.demo.item.infrastructure.batch;

import com.example.demo.item.application.port.BatchJobPersistencePort;
import com.example.demo.item.application.result.BatchJobStatus;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class BatchJobPersistenceAdapter implements BatchJobPersistencePort {

    private final EntityManager entityManager;

    @Override
    @Transactional
    public Long start(final String jobName) {
        final BatchJobExecutionEntity execution = new BatchJobExecutionEntity(jobName);
        entityManager.persist(execution);
        return execution.id();
    }

    @Override
    @Transactional
    public void recordItemError(
            final Long jobExecutionId,
            final Long itemId,
            final Integer channelId,
            final int attemptCount,
            final String errorType,
            final String errorMessage) {
        entityManager.persist(new BatchItemErrorEntity(
                jobExecutionId,
                itemId,
                channelId,
                attemptCount,
                errorType,
                errorMessage));
    }

    @Override
    @Transactional
    public void finish(
            final Long jobExecutionId,
            final BatchJobStatus status,
            final int totalRecords,
            final int successRecords,
            final String errorMessage) {
        final BatchJobExecutionEntity execution = entityManager.find(
                BatchJobExecutionEntity.class, jobExecutionId);
        if (execution == null) {
            throw new IllegalStateException("batch job execution not found");
        }
        execution.finish(status, totalRecords, successRecords, errorMessage);
    }
}
