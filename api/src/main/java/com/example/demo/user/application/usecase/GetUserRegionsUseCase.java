package com.example.demo.user.application.usecase;

import com.example.demo.user.application.port.RegionReferenceRepository;
import com.example.demo.user.application.port.UserRegionRepository;
import com.example.demo.user.application.query.GetUserRegionsQuery;
import com.example.demo.user.application.result.GetUserRegionsResult;
import com.example.demo.user.domain.UserRegion;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetUserRegionsUseCase {

    private final UserRegionRepository userRegionRepository;
    private final RegionReferenceRepository regionReferenceRepository;

    @Transactional(readOnly = true)
    public GetUserRegionsResult execute(final GetUserRegionsQuery query) {
        final List<UserRegion> userRegions = userRegionRepository.findAllByUserId(query.userId());
        final Map<String, String> regionNames = regionReferenceRepository.findNamesByIds(
                userRegions.stream().map(UserRegion::regionId).toList());
        return new GetUserRegionsResult(userRegions.stream()
                .map(userRegion -> new GetUserRegionsResult.Region(
                        userRegion.regionId(),
                        regionNames.get(userRegion.regionId()),
                        userRegion.isCurrent()))
                .toList());
    }
}
