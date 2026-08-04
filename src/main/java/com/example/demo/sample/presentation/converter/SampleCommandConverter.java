package com.example.demo.sample.presentation.converter;

import com.example.demo.sample.application.command.CreateSampleMessageCommand;
import com.example.demo.sample.presentation.dto.CreateSampleMessageRequest;
import org.springframework.stereotype.Component;

@Component
public class SampleCommandConverter {

    public CreateSampleMessageCommand toCreateSampleMessageCommand(
            final CreateSampleMessageRequest request) {
        return new CreateSampleMessageCommand(request.message());
    }
}
