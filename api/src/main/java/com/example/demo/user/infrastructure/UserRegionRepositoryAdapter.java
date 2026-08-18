package com.example.demo.user.infrastructure;

import com.example.demo.user.application.port.UserRegionRepository;
import com.example.demo.user.domain.UserRegion;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserRegionRepositoryAdapter implements UserRegionRepository {

    private final UserRegionJpaRepository userRegionJpaRepository;

    @Override
    public UserRegion saveAndFlush(final UserRegion userRegion) {
        return userRegionJpaRepository.saveAndFlush(userRegion);
    }

    @Override
    public boolean existsByUserIdAndRegionId(final Long userId, final String regionId) {
        return userRegionJpaRepository.existsByUserIdAndRegionId(userId, regionId);
    }

    @Override
    public long countByUserId(final Long userId) {
        return userRegionJpaRepository.countByUserId(userId);
    }

    @Override
    public Optional<UserRegion> findByUserIdAndRegionId(final Long userId, final String regionId) {
        return userRegionJpaRepository.findByUserIdAndRegionId(userId, regionId);
    }

    @Override
    public void clearCurrentByUserId(final Long userId) {
        userRegionJpaRepository.clearCurrentByUserId(userId);
    }
}
