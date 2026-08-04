package com.example.demo.sample.infrastructure;

import com.example.demo.sample.domain.SampleMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SampleMessageInitializer implements CommandLineRunner {

    private final SampleMessageJpaRepository sampleMessageJpaRepository;

    @Override
    public void run(final String... args) {
        if (sampleMessageJpaRepository.count() == 0) {
            sampleMessageJpaRepository.save(SampleMessage.defaultMessage());
        }
    }
}
