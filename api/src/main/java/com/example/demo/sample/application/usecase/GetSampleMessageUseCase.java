package com.example.demo.sample.application.usecase;

import com.example.demo.sample.application.result.SampleMessageResult;
import com.example.demo.sample.application.port.SampleMessageQueryPort;
import com.example.demo.sample.domain.SampleMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetSampleMessageUseCase {

    private final SampleMessageQueryPort sampleMessageQueryPort;

    @Transactional(readOnly = true)
    public SampleMessageResult execute() {
        final SampleMessage sampleMessage = sampleMessageQueryPort.findFirst()
                .orElseThrow(() -> new IllegalStateException("sample message not found"));
        return new SampleMessageResult(sampleMessage.message());
    }
}
