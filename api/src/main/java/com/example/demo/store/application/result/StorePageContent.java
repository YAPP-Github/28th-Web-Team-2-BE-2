package com.example.demo.store.application.result;

import java.util.List;

public record StorePageContent(
        String imageUrl,
        String imageContentType,
        byte[] imageContent,
        List<String> businessHours,
        String openStatus) {

    public StorePageContent {
        businessHours = businessHours == null ? List.of() : List.copyOf(businessHours);
        openStatus = openStatus == null ? "UNKNOWN" : openStatus;
    }

    public static StorePageContent empty() {
        return new StorePageContent(null, null, null, List.of(), "UNKNOWN");
    }
}
