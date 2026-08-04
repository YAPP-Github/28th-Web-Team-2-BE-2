package com.example.demo.sample.infrastructure;

import com.example.demo.sample.application.port.SampleMessageCommandPort;
import com.example.demo.sample.domain.SampleMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class SampleMessageCommandAdapter implements SampleMessageCommandPort {

    private final SampleMessageJpaRepository sampleMessageJpaRepository;

    @Override
    public SampleMessage save(final SampleMessage sampleMessage) {
        return sampleMessageJpaRepository.save(sampleMessage);
    }
}
