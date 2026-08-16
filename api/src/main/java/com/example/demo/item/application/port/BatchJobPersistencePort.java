package com.example.demo.item.application.port;

import com.example.demo.item.application.contract.BatchItemFailure;
import com.example.demo.item.application.contract.BatchJobCompletion;

public interface BatchJobPersistencePort {

    Long start(String jobName);

    void recordItemError(Long jobExecutionId, BatchItemFailure failure);

    void finish(Long jobExecutionId, BatchJobCompletion completion);
}
