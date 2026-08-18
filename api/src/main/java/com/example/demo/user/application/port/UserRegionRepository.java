package com.example.demo.user.application.port;

import com.example.demo.user.domain.UserRegion;
import java.util.Optional;

public interface UserRegionRepository {

    UserRegion saveAndFlush(UserRegion userRegion);

    boolean existsByUserIdAndRegionId(Long userId, String regionId);

    long countByUserId(Long userId);

    Optional<UserRegion> findByUserIdAndRegionId(Long userId, String regionId);

    void clearCurrentByUserId(Long userId);
}
