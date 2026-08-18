package com.example.demo.item.infrastructure.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class ThreadSleepBatchRetryDelayTest {

    @Test
    void 인터럽트_상태를_복원하고_실패한다() {
        final ThreadSleepBatchRetryDelay delay = new ThreadSleepBatchRetryDelay();
        Thread.currentThread().interrupt();

        try {
            assertThatThrownBy(() -> delay.delay(Duration.ZERO))
                    .isInstanceOf(IllegalStateException.class);
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }
    }
}
