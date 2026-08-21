package com.example.demo.store.application.result;

import java.util.List;

public record StoreDetailEnrichment(
        String storeImageUrl,
        List<String> businessHours,
        String openStatus) {

    public boolean hasValues() {
        return storeImageUrl != null || !hasNoHours() || isKnownStatus();
    }

    private boolean hasNoHours() {
        return businessHours == null || businessHours.isEmpty();
    }

    private boolean isKnownStatus() {
        return openStatus != null && !openStatus.isBlank() && !"UNKNOWN".equals(openStatus);
    }
}
