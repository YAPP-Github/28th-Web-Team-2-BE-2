package com.example.demo.user.presentation.dto;

import com.example.demo.user.domain.UserRank;

public record UserMeResponse(
        String nickname,
        CurrentRegion currentRegion,
        OnboardingStep onboardingStep,
        UserRank rank) {

    public record CurrentRegion(String regionId, String regionName) {}

    public enum OnboardingStep {
        NICKNAME,
        REGION,
        COMPLETED
    }
}
