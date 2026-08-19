package com.example.demo.user.application.result;

import java.util.List;

public record GetUserRegionsResult(List<Region> regions) {

    public record Region(String regionId, String regionName, boolean isCurrent) {}
}
