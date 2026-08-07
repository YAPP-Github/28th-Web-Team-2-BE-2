package com.example.demo.sample.presentation.converter;

import com.example.demo.sample.application.result.SampleMessageResult;
import com.example.demo.sample.presentation.dto.SampleMessageResponse;
import org.springframework.stereotype.Component;

@Component
public class SampleResultConverter {

    public SampleMessageResponse toSampleMessageResponse(final SampleMessageResult result) {
        return new SampleMessageResponse(result.message());
    }
}
