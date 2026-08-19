package com.example.demo.user.presentation.dto;

import java.util.List;

public record UserRegionsResponse(List<Region> regions) {

    public record Region(String regionId, String regionName, boolean isCurrent) {}
}
