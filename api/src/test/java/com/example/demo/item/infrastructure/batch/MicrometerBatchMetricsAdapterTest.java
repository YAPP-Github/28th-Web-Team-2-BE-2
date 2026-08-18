package com.example.demo.item.infrastructure.batch;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.item.application.port.BatchExecutionOutcome;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class MicrometerBatchMetricsAdapterTest {

    @Test
    void 실행_재시도_소요시간을_지정된_이름과_낮은_카디널리티_태그로_기록한다() {
        final SimpleMeterRegistry registry = new SimpleMeterRegistry();
        final MicrometerBatchMetricsAdapter adapter = new MicrometerBatchMetricsAdapter(registry);

        adapter.recordExecution("ONLINE_PRICE_COLLECTION", "OASIS", BatchExecutionOutcome.SUCCESS);
        adapter.recordRetries("ONLINE_PRICE_COLLECTION", "OASIS", 4);
        adapter.recordDuration("ONLINE_PRICE_COLLECTION", "OASIS", Duration.ofSeconds(3));

        assertThat(registry.get("batch.job.executions").counter().count()).isEqualTo(1.0);
        assertThat(registry.get("batch.job.retries").counter().count()).isEqualTo(4.0);
        assertThat(registry.get("batch.job.duration").timer().count()).isEqualTo(1);
        assertThat(registry.getMeters())
                .extracting(Meter::getId)
                .extracting(Meter.Id::getName)
                .containsExactlyInAnyOrder("batch.job.executions", "batch.job.retries", "batch.job.duration");
        assertThat(registry.getMeters())
                .flatExtracting(meter -> meter.getId().getTags())
                .extracting(tag -> tag.getKey())
                .containsOnly("job", "channel", "outcome");
    }

    @Test
    void 성공_실패_스킵_결과를_각각_기록한다() {
        final SimpleMeterRegistry registry = new SimpleMeterRegistry();
        final MicrometerBatchMetricsAdapter adapter = new MicrometerBatchMetricsAdapter(registry);

        adapter.recordExecution("ONLINE_PRICE_COLLECTION", "OASIS", BatchExecutionOutcome.SUCCESS);
        adapter.recordExecution("ONLINE_PRICE_COLLECTION", "OASIS", BatchExecutionOutcome.FAILURE);
        adapter.recordExecution("ONLINE_PRICE_COLLECTION", "OASIS", BatchExecutionOutcome.SKIP);

        assertThat(registry.get("batch.job.executions")
                .tag("job", "ONLINE_PRICE_COLLECTION")
                .tag("channel", "OASIS")
                .tag("outcome", "success")
                .counter()
                .count()).isEqualTo(1.0);
        assertThat(registry.get("batch.job.executions")
                .tag("job", "ONLINE_PRICE_COLLECTION")
                .tag("channel", "OASIS")
                .tag("outcome", "failure")
                .counter()
                .count()).isEqualTo(1.0);
        assertThat(registry.get("batch.job.executions")
                .tag("job", "ONLINE_PRICE_COLLECTION")
                .tag("channel", "OASIS")
                .tag("outcome", "skip")
                .counter()
                .count()).isEqualTo(1.0);
    }
}
