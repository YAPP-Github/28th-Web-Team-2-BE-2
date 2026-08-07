package com.example.demo.price.infrastructure.batch;

import com.example.demo.price.application.port.CollectionTaskProvider;
import com.example.demo.price.application.result.CollectionResult;
import com.example.demo.price.application.usecase.CollectPriceTaskUseCase;
import com.example.demo.price.application.command.CollectionTask;
import java.time.LocalDate;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class PriceCollectionBatchConfiguration {

    @Bean
    Job priceCollectionJob(final JobRepository jobRepository, final Step priceCollectionStep) {
        return new JobBuilder("priceCollectionJob", jobRepository)
                .start(priceCollectionStep)
                .build();
    }

    @Bean
    @StepScope
    CollectionTaskReader collectionTaskReader(
            final CollectionTaskProvider taskProvider,
            @Value("#{jobParameters['priceDate']}") final String priceDate,
            @Value("#{stepExecution.jobExecution.id}") final Long executionId) {
        return new CollectionTaskReader(taskProvider, LocalDate.parse(priceDate), executionId);
    }

    @Bean
    Step priceCollectionStep(
            final JobRepository jobRepository,
            final PlatformTransactionManager transactionManager,
            final CollectPriceTaskUseCase useCase,
            final CollectionTaskReader reader) {
        final ItemProcessor<CollectionTask, CollectionResult> processor = useCase::execute;
        final ItemWriter<CollectionResult> writer = chunk -> { };
        return new StepBuilder("priceCollectionStep", jobRepository)
                .<CollectionTask, CollectionResult>chunk(1, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .faultTolerant()
                .skipLimit(Integer.MAX_VALUE)
                .skip(RuntimeException.class)
                .build();
    }
}
