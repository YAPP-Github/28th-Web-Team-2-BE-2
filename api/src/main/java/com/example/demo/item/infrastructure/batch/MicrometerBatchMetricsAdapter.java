package com.example.demo.item.infrastructure.batch;

import com.example.demo.item.application.port.BatchExecutionOutcome;
import com.example.demo.item.application.port.BatchMetricsPort;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MicrometerBatchMetricsAdapter implements BatchMetricsPort {

    private static final String EXECUTION_METRIC = "batch.job.executions";
    private static final String RETRY_METRIC = "batch.job.retries";
    private static final String DURATION_METRIC = "batch.job.duration";

    private final MeterRegistry meterRegistry;

    @Override
    public void recordExecution(
            final String job,
            final String channel,
            final BatchExecutionOutcome outcome) {
        meterRegistry.counter(
                        EXECUTION_METRIC,
                        "job", job,
                        "channel", channel,
                        "outcome", outcome.tagValue())
                .increment();
    }

    @Override
    public void recordRetries(
            final String job,
            final String channel,
            final int retryCount) {
        meterRegistry.counter(RETRY_METRIC, "job", job, "channel", channel)
                .increment(retryCount);
    }

    @Override
    public void recordDuration(
            final String job,
            final String channel,
            final Duration duration) {
        meterRegistry.timer(DURATION_METRIC, "job", job, "channel", channel)
                .record(duration);
    }
}
