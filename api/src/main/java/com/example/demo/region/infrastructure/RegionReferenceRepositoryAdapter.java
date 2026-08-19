package com.example.demo.region.infrastructure;

import com.example.demo.region.domain.Region;
import com.example.demo.user.application.port.RegionReferenceRepository;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
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

    @Override
    public Map<String, String> findNamesByIds(final Collection<String> regionIds) {
        return regionJpaRepository.findAllById(regionIds).stream()
                .collect(Collectors.toMap(Region::regionId, Region::regionName));
    }
}
