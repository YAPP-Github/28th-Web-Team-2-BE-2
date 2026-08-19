package com.example.demo.user.presentation.converter;

import com.example.demo.user.application.result.GetUserMeResult;
import com.example.demo.user.presentation.dto.UserMeResponse;
import org.springframework.stereotype.Component;

@Component
public class UserResultConverter {

    public UserMeResponse toUserMeResponse(final GetUserMeResult result) {
        return new UserMeResponse(
                result.nickname(),
                toCurrentRegion(result.currentRegion()),
                toOnboardingStep(result.onboardingStep()));
    }

    private UserMeResponse.OnboardingStep toOnboardingStep(
            final GetUserMeResult.OnboardingStep onboardingStep) {
        return switch (onboardingStep) {
            case NICKNAME -> UserMeResponse.OnboardingStep.NICKNAME;
            case REGION -> UserMeResponse.OnboardingStep.REGION;
            case COMPLETED -> UserMeResponse.OnboardingStep.COMPLETED;
        };
    }

    private UserMeResponse.CurrentRegion toCurrentRegion(
            final GetUserMeResult.CurrentRegion currentRegion) {
        if (currentRegion == null) {
            return null;
        }
        return new UserMeResponse.CurrentRegion(currentRegion.regionId(), currentRegion.regionName());
    }
}
