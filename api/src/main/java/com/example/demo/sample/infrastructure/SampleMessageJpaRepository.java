package com.example.demo.sample.infrastructure;

import com.example.demo.sample.domain.SampleMessage;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SampleMessageJpaRepository extends JpaRepository<SampleMessage, Long> {

    Optional<SampleMessage> findFirstByOrderByIdAsc();
}
