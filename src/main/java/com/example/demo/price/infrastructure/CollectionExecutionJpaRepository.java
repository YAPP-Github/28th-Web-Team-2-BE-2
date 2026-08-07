package com.example.demo.price.infrastructure;

import com.example.demo.price.domain.CollectionExecutionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface CollectionExecutionJpaRepository
        extends JpaRepository<CollectionExecutionEntity, Long> {

    @Transactional
    void deleteAllByChannel(String channel);
}
