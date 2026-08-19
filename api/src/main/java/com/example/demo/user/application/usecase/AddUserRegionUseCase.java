package com.example.demo.user.application.usecase;

import com.example.demo.auth.application.port.UserRepository;
import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import com.example.demo.user.application.command.AddUserRegionCommand;
import com.example.demo.user.application.port.RegionReferenceRepository;
import com.example.demo.user.application.port.UserRegionRepository;
import com.example.demo.user.domain.UserRegion;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AddUserRegionUseCase {

    private static final int MAX_USER_REGIONS = 3;

    private final UserRepository userRepository;
    private final UserRegionRepository userRegionRepository;
    private final RegionReferenceRepository regionReferenceRepository;

    @Transactional
    public void execute(final AddUserRegionCommand command) {
        validateRegion(command.regionId());
        findUserForUpdate(command.userId());
        validateNotDuplicate(command.userId(), command.regionId());
        validateLimit(command.userId());
        save(command);
    }

    private void validateRegion(final String regionId) {
        if (!regionReferenceRepository.existsById(regionId)) {
            throw ApiException.invalidParameter();
        }
    }

    private void findUserForUpdate(final Long userId) {
        userRepository.findByIdForUpdate(userId).orElseThrow(() -> new ApiException(
                ErrorType.NO_RESOURCE_ERROR.description(),
                ErrorType.NO_RESOURCE_ERROR,
                HttpStatus.NOT_FOUND));
    }

    private void validateNotDuplicate(final Long userId, final String regionId) {
        if (userRegionRepository.existsByUserIdAndRegionId(userId, regionId)) {
            throw new ApiException(
                    ErrorType.DUPLICATE_USER_REGION_ERROR.description(),
                    ErrorType.DUPLICATE_USER_REGION_ERROR,
                    HttpStatus.CONFLICT);
        }
    }

    private void validateLimit(final Long userId) {
        if (userRegionRepository.countByUserId(userId) >= MAX_USER_REGIONS) {
            throw new ApiException(
                    ErrorType.USER_REGION_LIMIT_EXCEEDED_ERROR.description(),
                    ErrorType.USER_REGION_LIMIT_EXCEEDED_ERROR,
                    HttpStatus.CONFLICT);
        }
    }

    private void save(final AddUserRegionCommand command) {
        try {
            userRegionRepository.saveAndFlush(
                    UserRegion.interestedIn(command.userId(), command.regionId()));
        } catch (final DataIntegrityViolationException exception) {
            throw new ApiException(
                    ErrorType.DUPLICATE_USER_REGION_ERROR.description(),
                    ErrorType.DUPLICATE_USER_REGION_ERROR,
                    HttpStatus.CONFLICT,
                    exception);
        }
    }
}
