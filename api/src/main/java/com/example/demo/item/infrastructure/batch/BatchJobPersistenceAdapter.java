package com.example.demo.item.infrastructure.batch;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import com.example.demo.item.application.contract.BatchItemFailure;
import com.example.demo.item.application.contract.BatchJobCompletion;
import com.example.demo.item.application.port.BatchJobPersistencePort;
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
            final BatchItemFailure failure) {
        entityManager.persist(new BatchItemErrorEntity(
                jobExecutionId,
                failure,
                errorType(failure.cause())));
    }

    @Override
    @Transactional
    public void finish(
            final Long jobExecutionId,
            final BatchJobCompletion completion) {
        final BatchJobExecutionEntity execution = entityManager.find(
                BatchJobExecutionEntity.class, jobExecutionId);
        if (execution == null) {
            throw new IllegalStateException("batch job execution not found");
        }
        execution.finish(completion);
    }

    private ErrorType errorType(final RuntimeException cause) {
        if (cause instanceof ApiException apiException) {
            return apiException.errorType();
        }
        return ErrorType.UNKNOWN_ERROR;
    }
}
