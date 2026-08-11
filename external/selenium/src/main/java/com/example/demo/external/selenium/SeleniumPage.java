package com.example.demo.external.selenium;

import java.net.URI;
import java.util.Objects;

public record SeleniumPage(URI sourceUrl, String html) {

    public SeleniumPage {
        Objects.requireNonNull(sourceUrl, "sourceUrl must not be null");
        Objects.requireNonNull(html, "html must not be null");
    }
}
