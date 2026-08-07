package com.example.demo.sample.application.port;

import com.example.demo.sample.domain.SampleMessage;

@FunctionalInterface
public interface SampleMessageCommandPort {
    SampleMessage save(SampleMessage sampleMessage);
}
