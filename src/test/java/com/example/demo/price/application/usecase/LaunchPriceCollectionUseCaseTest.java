package com.example.demo.price.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.price.application.port.PriceCollectionJobLauncher;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class LaunchPriceCollectionUseCaseTest {

    @Test
    void 지정한_수집일로_batch_job을_실행한다() {
        final AtomicReference<LocalDate> captured = new AtomicReference<>();
        final PriceCollectionJobLauncher launcher = priceDate -> {
            captured.set(priceDate);
        };

        new LaunchPriceCollectionUseCase(launcher)
                .execute(LocalDate.of(2026, 8, 7));

        assertThat(captured.get()).isEqualTo(LocalDate.of(2026, 8, 7));
    }
}
