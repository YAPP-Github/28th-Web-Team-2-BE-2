package com.example.demo.sample.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.sample.application.command.CreateSampleMessageCommand;
import com.example.demo.sample.application.result.SampleMessageResult;
import com.example.demo.sample.domain.SampleMessage;
import com.example.demo.sample.infrastructure.SampleMessageJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CreateSampleMessageUseCaseIntegrationTest {

    @Autowired
    private CreateSampleMessageUseCase createSampleMessageUseCase;

    @Autowired
    private SampleMessageJpaRepository sampleMessageJpaRepository;

    @BeforeEach
    void setUp() {
        sampleMessageJpaRepository.deleteAll();
    }

    @Test
    void 메시지를_생성하면_실제_DB에_저장된다() {
        final SampleMessageResult result = createSampleMessageUseCase.execute(
                new CreateSampleMessageCommand("Persisted from service"));

        final SampleMessage savedSampleMessage = sampleMessageJpaRepository
                .findFirstByOrderByIdAsc()
                .orElseThrow();

        assertThat(result.message()).isEqualTo("Persisted from service");
        assertThat(savedSampleMessage.message()).isEqualTo("Persisted from service");
    }
}
