package com.example.demo.sample.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.sample.application.port.SampleMessageCommandPort;
import com.example.demo.sample.application.port.SampleMessageQueryPort;
import com.example.demo.sample.domain.SampleMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SampleMessageInfrastructureTest {

    @Autowired
    private SampleMessageJpaRepository sampleMessageJpaRepository;

    @Autowired
    private SampleMessageCommandPort sampleMessageCommandPort;

    @Autowired
    private SampleMessageQueryPort sampleMessageQueryPort;

    @BeforeEach
    void setUp() {
        sampleMessageJpaRepository.deleteAll();
    }

    @Test
    void JPA로_저장한_sample을_조회한다() {
        sampleMessageCommandPort.save(new SampleMessage("Persisted from JPA"));

        final SampleMessage result = sampleMessageQueryPort.findFirst().orElseThrow();

        assertThat(result.message()).isEqualTo("Persisted from JPA");
    }
}
