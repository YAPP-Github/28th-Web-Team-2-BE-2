package com.example.demo.price.infrastructure.batch;

import com.example.demo.price.application.port.PriceCollectionJobLauncher;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SpringBatchPriceCollectionJobLauncher implements PriceCollectionJobLauncher {

    private final JobLauncher jobLauncher;
    private final Job priceCollectionJob;

    @Override
    public void launch(final LocalDate priceDate) {
        final JobParameters parameters = new JobParametersBuilder()
                .addString("priceDate", priceDate.toString())
                .addString("executionKey", UUID.randomUUID().toString())
                .toJobParameters();
        try {
            jobLauncher.run(priceCollectionJob, parameters);
        } catch (Exception exception) {
            throw new IllegalStateException("failed to launch price collection job", exception);
        }
    }
}
