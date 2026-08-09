package com.example.demo.external.selenium;

import java.net.URI;
import java.util.Objects;

public class SeleniumPageLoadException extends RuntimeException {

    private final URI targetUrl;

    public SeleniumPageLoadException(
            final URI targetUrl,
            final String message,
            final Throwable cause) {
        super(message, cause);
        this.targetUrl = Objects.requireNonNull(targetUrl, "targetUrl must not be null");
    }

    public URI targetUrl() {
        return targetUrl;
    }
}
