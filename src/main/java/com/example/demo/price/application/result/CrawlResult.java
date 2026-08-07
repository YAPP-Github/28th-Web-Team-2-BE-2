package com.example.demo.price.application.result;

import com.example.demo.price.domain.RawOffer;
import java.time.OffsetDateTime;
import java.util.List;

public record CrawlResult(
        List<RawOffer> offers,
        String sourceUrl,
        OffsetDateTime collectedAt,
        AccessStatus accessStatus,
        String failureReason) {

    public CrawlResult {
        offers = offers == null ? List.of() : List.copyOf(offers);
        if (collectedAt == null || accessStatus == null) {
            throw new IllegalArgumentException("crawl result metadata must not be null");
        }
    }

    public enum AccessStatus {
        SUCCESS,
        TEMPORARY_FAILURE,
        BLOCKED
    }
}
