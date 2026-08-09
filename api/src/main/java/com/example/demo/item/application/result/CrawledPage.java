package com.example.demo.item.application.result;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Objects;

public record CrawledPage(
        URI sourceUrl,
        String html,
        OffsetDateTime collectedAt,
        CrawlStatus status,
        String failureReason) {

    public CrawledPage {
        Objects.requireNonNull(sourceUrl, "sourceUrl must not be null");
        Objects.requireNonNull(collectedAt, "collectedAt must not be null");
        Objects.requireNonNull(status, "status must not be null");
        if (html == null) {
            html = "";
        }
    }

    public static CrawledPage success(
            final URI sourceUrl,
            final String html,
            final OffsetDateTime collectedAt) {
        return new CrawledPage(sourceUrl, html, collectedAt, CrawlStatus.SUCCESS, null);
    }

    public static CrawledPage temporaryFailure(
            final URI sourceUrl,
            final OffsetDateTime collectedAt,
            final String failureReason) {
        return new CrawledPage(
                sourceUrl, "", collectedAt, CrawlStatus.TEMPORARY_FAILURE, failureReason);
    }

    public enum CrawlStatus {
        SUCCESS,
        TEMPORARY_FAILURE
    }
}
