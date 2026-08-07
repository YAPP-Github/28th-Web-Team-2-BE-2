package com.example.demo.price.infrastructure;

import com.example.demo.price.application.port.CollectionExecutionRepository;
import com.example.demo.price.domain.CollectionExecutionEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CollectionExecutionRepositoryAdapter implements CollectionExecutionRepository {

    private final CollectionExecutionJpaRepository repository;

    @Override
    public void record(final TaskExecution execution) {
        repository.save(new CollectionExecutionEntity(
                execution.executionId(), execution.itemId(), execution.itemName(), execution.channel(),
                execution.status(), execution.startedAt(), execution.finishedAt(),
                execution.validOfferCount(), execution.failureReason()));
    }
}
