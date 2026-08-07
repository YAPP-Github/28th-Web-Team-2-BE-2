package com.example.demo.sample.presentation;

import com.example.demo.sample.application.command.CreateSampleMessageCommand;
import com.example.demo.sample.application.result.SampleMessageResult;
import com.example.demo.sample.application.usecase.CreateSampleMessageUseCase;
import com.example.demo.sample.application.usecase.GetSampleMessageUseCase;
import com.example.demo.sample.presentation.converter.SampleCommandConverter;
import com.example.demo.sample.presentation.converter.SampleResultConverter;
import com.example.demo.sample.presentation.dto.CreateSampleMessageRequest;
import com.example.demo.sample.presentation.dto.SampleMessageResponse;
import com.example.demo.sample.presentation.spec.SampleControllerSpec;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/samples")
@RequiredArgsConstructor
public class SampleController implements SampleControllerSpec {

    private final CreateSampleMessageUseCase createSampleMessageUseCase;
    private final GetSampleMessageUseCase getSampleMessageUseCase;
    private final SampleCommandConverter commandConverter;
    private final SampleResultConverter resultConverter;

    @PostMapping
    @Override
    public ResponseEntity<SampleMessageResponse> createSampleMessage(
            @Valid @RequestBody final CreateSampleMessageRequest request) {
        final CreateSampleMessageCommand command = commandConverter.toCreateSampleMessageCommand(request);
        final SampleMessageResult result = createSampleMessageUseCase.execute(command);
        final SampleMessageResponse response = resultConverter.toSampleMessageResponse(result);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Override
    public ResponseEntity<SampleMessageResponse> getSampleMessage() {
        final SampleMessageResult result = getSampleMessageUseCase.execute();
        final SampleMessageResponse response = resultConverter.toSampleMessageResponse(result);
        return ResponseEntity.ok(response);
    }
}
