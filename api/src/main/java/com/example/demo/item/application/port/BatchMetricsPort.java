package com.example.demo.item.application.port;

import java.time.Duration;

public interface BatchMetricsPort {

    void recordExecution(String job, String channel, Outcome outcome);

    void recordRetries(String job, String channel, int retryCount);

    void recordDuration(String job, String channel, Duration duration);

    enum Outcome {
        SUCCESS("success"),
        FAILURE("failure"),
        SKIP("skip");

        private final String tagValue;

        Outcome(final String tagValue) {
            this.tagValue = tagValue;
        }

        public String tagValue() {
            return tagValue;
        }
    }
}
