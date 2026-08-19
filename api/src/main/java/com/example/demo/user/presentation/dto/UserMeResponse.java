package com.example.demo.user.presentation.dto;

public record UserMeResponse(
        String nickname,
        CurrentRegion currentRegion,
        OnboardingStep onboardingStep) {

    public record CurrentRegion(String regionId, String regionName) {}

    public enum OnboardingStep {
        NICKNAME,
        REGION,
        COMPLETED
    }
}
