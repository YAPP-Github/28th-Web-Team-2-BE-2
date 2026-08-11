package com.example.demo.item.application.result;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Objects;

public record CrawlOnlineItemResult(
        URI sourceUrl,
        String html,
        OffsetDateTime collectedAt,
        OnlineItemCrawlStatus status,
        String failureReason) {

    public CrawlOnlineItemResult {
        Objects.requireNonNull(sourceUrl, "sourceUrl must not be null");
        Objects.requireNonNull(collectedAt, "collectedAt must not be null");
        Objects.requireNonNull(status, "status must not be null");
        if (html == null) {
            html = "";
        }
    }

    public static CrawlOnlineItemResult success(
            final URI sourceUrl,
            final String html,
            final OffsetDateTime collectedAt) {
        return new CrawlOnlineItemResult(
                sourceUrl, html, collectedAt, OnlineItemCrawlStatus.SUCCESS, null);
    }

    public static CrawlOnlineItemResult temporaryFailure(
            final URI sourceUrl,
            final OffsetDateTime collectedAt,
            final String failureReason) {
        return new CrawlOnlineItemResult(
                sourceUrl, "", collectedAt, OnlineItemCrawlStatus.TEMPORARY_FAILURE, failureReason);
    }
}
