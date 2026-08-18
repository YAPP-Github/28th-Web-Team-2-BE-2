package com.example.demo.item.application.port;

import java.time.Duration;

public interface BatchMetricsPort {

    void recordExecution(String job, String channel, BatchExecutionOutcome outcome);

    void recordRetries(String job, String channel, int retryCount);

    void recordDuration(String job, String channel, Duration duration);
}
