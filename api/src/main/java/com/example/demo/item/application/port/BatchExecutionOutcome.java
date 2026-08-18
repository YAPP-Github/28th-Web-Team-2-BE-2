package com.example.demo.item.application.port;

public enum BatchExecutionOutcome {
    SUCCESS("success"),
    FAILURE("failure"),
    SKIP("skip");

    private final String tagValue;

    BatchExecutionOutcome(final String tagValue) {
        this.tagValue = tagValue;
    }

    public String tagValue() {
        return tagValue;
    }
}
