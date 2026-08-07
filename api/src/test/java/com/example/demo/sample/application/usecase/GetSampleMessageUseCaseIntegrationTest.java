package com.example.demo.sample.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.sample.application.result.SampleMessageResult;
import com.example.demo.sample.domain.SampleMessage;
import com.example.demo.sample.infrastructure.SampleMessageJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class GetSampleMessageUseCaseIntegrationTest {

    @Autowired
    private GetSampleMessageUseCase getSampleMessageUseCase;

    @Autowired
    private SampleMessageJpaRepository sampleMessageJpaRepository;

    @BeforeEach
    void setUp() {
        sampleMessageJpaRepository.deleteAll();
    }

    @Test
    void 실제_DB의_메시지를_조회한다() {
        sampleMessageJpaRepository.save(new SampleMessage("Read from service"));

        final SampleMessageResult result = getSampleMessageUseCase.execute();

        assertThat(result.message()).isEqualTo("Read from service");
    }
}
