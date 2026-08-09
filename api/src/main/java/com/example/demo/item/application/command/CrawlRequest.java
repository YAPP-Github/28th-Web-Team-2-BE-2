package com.example.demo.item.application.command;

import java.net.URI;

public record CrawlRequest(URI targetUrl) {

    public CrawlRequest {
        if (targetUrl == null || !targetUrl.isAbsolute()) {
            throw new IllegalArgumentException("target url must be an absolute URI");
        }
    }
}
