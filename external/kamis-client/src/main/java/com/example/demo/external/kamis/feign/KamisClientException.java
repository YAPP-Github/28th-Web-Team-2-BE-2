package com.example.demo.external.kamis.feign;

public final class KamisClientException extends RuntimeException {

    private final int status;

    public KamisClientException(final int status, final String message) {
        super(message);
        this.status = status;
    }

    public int status() {
        return status;
    }
}
