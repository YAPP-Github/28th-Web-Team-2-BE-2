package com.example.demo.region.infrastructure;

import com.example.demo.user.application.port.RegionReferenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RegionReferenceRepositoryAdapter implements RegionReferenceRepository {

    private final RegionJpaRepository regionJpaRepository;

    @Override
    public boolean existsById(final String regionId) {
        return regionJpaRepository.existsById(regionId);
    }
}
