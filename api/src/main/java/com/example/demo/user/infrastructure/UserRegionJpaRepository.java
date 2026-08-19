package com.example.demo.user.infrastructure;

import com.example.demo.user.domain.UserRegion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRegionJpaRepository extends JpaRepository<UserRegion, Long> {

    boolean existsByUserIdAndRegionId(Long userId, String regionId);

    long countByUserId(Long userId);

    Optional<UserRegion> findByUserIdAndRegionId(Long userId, String regionId);

    List<UserRegion> findAllByUserId(Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update UserRegion region set region.isCurrent = false where region.userId = :userId")
    void clearCurrentByUserId(@Param("userId") Long userId);
}
