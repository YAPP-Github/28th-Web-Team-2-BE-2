package com.example.demo.sample.infrastructure;

import com.example.demo.sample.application.port.SampleMessageQueryPort;
import com.example.demo.sample.domain.SampleMessage;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class SampleMessageQueryAdapter implements SampleMessageQueryPort {

    private final SampleMessageJpaRepository sampleMessageJpaRepository;

    @Override
    public Optional<SampleMessage> findFirst() {
        return sampleMessageJpaRepository.findFirstByOrderByIdAsc();
    }
}
