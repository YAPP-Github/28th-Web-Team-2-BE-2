package com.example.demo.item.application.command;

import com.example.demo.common.exception.ApiException;
import java.net.URI;

public record CrawlOnlineItemCommand(URI targetUrl) {

    public CrawlOnlineItemCommand {
        if (targetUrl == null || !targetUrl.isAbsolute()) {
            throw ApiException.invalidParameter();
        }
    }
}
