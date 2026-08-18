package com.example.demo.region.infrastructure;

import com.example.demo.region.application.port.RegionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RegionRepositoryAdapter implements RegionRepository {

    private final RegionJpaRepository regionJpaRepository;

    @Override
    public boolean existsById(final String regionId) {
        return regionJpaRepository.existsById(regionId);
    }
}
