package com.example.demo.user.application.usecase;

import com.example.demo.auth.application.port.UserRepository;
import com.example.demo.auth.domain.User;
import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import com.example.demo.user.application.port.UserReportCountQueryPort;
import com.example.demo.user.application.query.GetUserRegionsQuery;
import com.example.demo.user.application.result.GetUserMeResult;
import com.example.demo.user.application.result.GetUserRegionsResult;
import com.example.demo.user.domain.UserRank;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetUserMeUseCase {

    private final UserRepository userRepository;
    private final GetUserRegionsUseCase getUserRegionsUseCase;
    private final UserReportCountQueryPort userReportCountQueryPort;

    @Transactional(readOnly = true)
    public GetUserMeResult execute(final Long userId) {
        final User user = userRepository.findById(userId).orElseThrow(() -> new ApiException(
                ErrorType.NO_RESOURCE_ERROR.description(),
                ErrorType.NO_RESOURCE_ERROR,
                HttpStatus.NOT_FOUND));
        final GetUserRegionsResult userRegions = getUserRegionsUseCase.execute(
                new GetUserRegionsQuery(userId));
        final GetUserMeResult.CurrentRegion currentRegion = findCurrentRegion(userRegions);
        final long reportCount = userReportCountQueryPort.findReportCounts(Set.of(userId))
                .getOrDefault(userId, 0L);
        return new GetUserMeResult(
                user.nickname(), currentRegion, determineOnboardingStep(user.nickname(), currentRegion),
                UserRank.fromReportCount(reportCount));
    }

    private GetUserMeResult.CurrentRegion findCurrentRegion(final GetUserRegionsResult userRegions) {
        return userRegions.regions().stream()
                .filter(GetUserRegionsResult.Region::isCurrent)
                .findFirst()
                .map(region -> new GetUserMeResult.CurrentRegion(region.regionId(), region.regionName()))
                .orElse(null);
    }

    private GetUserMeResult.OnboardingStep determineOnboardingStep(
            final String nickname,
            final GetUserMeResult.CurrentRegion currentRegion) {
        if (nickname == null) {
            return GetUserMeResult.OnboardingStep.NICKNAME;
        }
        if (currentRegion == null) {
            return GetUserMeResult.OnboardingStep.REGION;
        }
        return GetUserMeResult.OnboardingStep.COMPLETED;
    }
}
