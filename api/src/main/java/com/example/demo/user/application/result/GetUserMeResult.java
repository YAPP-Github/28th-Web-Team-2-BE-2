package com.example.demo.user.application.result;

public record GetUserMeResult(
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
