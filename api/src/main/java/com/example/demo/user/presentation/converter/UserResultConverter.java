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
                UserMeResponse.OnboardingStep.valueOf(result.onboardingStep().name()));
    }

    private UserMeResponse.CurrentRegion toCurrentRegion(
            final GetUserMeResult.CurrentRegion currentRegion) {
        if (currentRegion == null) {
            return null;
        }
        return new UserMeResponse.CurrentRegion(currentRegion.regionId(), currentRegion.regionName());
    }
}
