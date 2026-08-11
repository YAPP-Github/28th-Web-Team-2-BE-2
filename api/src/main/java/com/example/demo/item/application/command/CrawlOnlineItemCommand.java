package com.example.demo.item.application.command;

import com.example.demo.common.exception.ApiException;
import java.net.URI;

public record CrawlOnlineItemCommand(URI targetUrl) {

    public CrawlOnlineItemCommand {
        if (targetUrl == null || !targetUrl.isAbsolute() || !hasHttpScheme(targetUrl) || targetUrl.getHost() == null) {
            throw ApiException.invalidParameter();
        }
    }

    private static boolean hasHttpScheme(final URI targetUrl) {
        return "http".equalsIgnoreCase(targetUrl.getScheme())
                || "https".equalsIgnoreCase(targetUrl.getScheme());
    }
}
