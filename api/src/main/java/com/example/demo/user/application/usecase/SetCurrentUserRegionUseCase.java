package com.example.demo.user.application.usecase;

import com.example.demo.auth.application.port.UserRepository;
import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import com.example.demo.region.application.port.RegionRepository;
import com.example.demo.user.application.command.SetCurrentUserRegionCommand;
import com.example.demo.user.application.port.UserRegionRepository;
import com.example.demo.user.domain.UserRegion;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SetCurrentUserRegionUseCase {

    private static final int MAX_USER_REGIONS = 3;

    private final UserRepository userRepository;
    private final UserRegionRepository userRegionRepository;
    private final RegionRepository regionRepository;

    @Transactional
    public void execute(final SetCurrentUserRegionCommand command) {
        validateRegion(command.regionId());
        findUserForUpdate(command.userId());
        userRegionRepository.clearCurrentByUserId(command.userId());
        final UserRegion userRegion = userRegionRepository
                .findByUserIdAndRegionId(command.userId(), command.regionId())
                .orElseGet(() -> createRegion(command));
        userRegion.markCurrent();
        userRegionRepository.saveAndFlush(userRegion);
    }

    private void validateRegion(final String regionId) {
        if (!regionRepository.existsById(regionId)) {
            throw ApiException.invalidParameter();
        }
    }

    private void findUserForUpdate(final Long userId) {
        userRepository.findByIdForUpdate(userId).orElseThrow(() -> new ApiException(
                ErrorType.NO_RESOURCE_ERROR.description(),
                ErrorType.NO_RESOURCE_ERROR,
                HttpStatus.NOT_FOUND));
    }

    private UserRegion createRegion(final SetCurrentUserRegionCommand command) {
        validateLimit(command.userId());
        return UserRegion.current(command.userId(), command.regionId());
    }

    private void validateLimit(final Long userId) {
        if (userRegionRepository.countByUserId(userId) >= MAX_USER_REGIONS) {
            throw new ApiException(
                    ErrorType.USER_REGION_LIMIT_EXCEEDED_ERROR.description(),
                    ErrorType.USER_REGION_LIMIT_EXCEEDED_ERROR,
                    HttpStatus.CONFLICT);
        }
    }
}
