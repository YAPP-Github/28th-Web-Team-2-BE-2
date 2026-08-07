package com.example.demo.sample.application.port;

import com.example.demo.sample.domain.SampleMessage;
import java.util.Optional;

@FunctionalInterface
public interface SampleMessageQueryPort {

    Optional<SampleMessage> findFirst();
}
