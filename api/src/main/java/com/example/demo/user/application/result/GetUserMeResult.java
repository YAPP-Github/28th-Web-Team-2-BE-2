package com.example.demo.user.application.result;

import com.example.demo.user.domain.UserRank;

public record GetUserMeResult(
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
