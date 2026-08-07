package com.example.demo.sample.application.usecase;

import com.example.demo.sample.application.command.CreateSampleMessageCommand;
import com.example.demo.sample.application.port.SampleMessageCommandPort;
import com.example.demo.sample.application.result.SampleMessageResult;
import com.example.demo.sample.domain.SampleMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateSampleMessageUseCase {

    private final SampleMessageCommandPort sampleMessageCommandPort;

    @Transactional
    public SampleMessageResult execute(final CreateSampleMessageCommand command) {
        final SampleMessage sampleMessage = new SampleMessage(command.message());
        final SampleMessage savedSampleMessage = sampleMessageCommandPort.save(sampleMessage);
        return new SampleMessageResult(savedSampleMessage.message());
    }
}
