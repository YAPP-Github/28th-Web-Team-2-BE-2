package com.example.demo.user.presentation.converter;

import com.example.demo.user.application.result.GetUserRegionsResult;
import com.example.demo.user.presentation.dto.UserRegionsResponse;
import org.springframework.stereotype.Component;

@Component
public class UserRegionResultConverter {

    public UserRegionsResponse toResponse(final GetUserRegionsResult result) {
        return new UserRegionsResponse(result.regions().stream()
                .map(region -> new UserRegionsResponse.Region(
                        region.regionId(), region.regionName(), region.isCurrent()))
                .toList());
    }
}
