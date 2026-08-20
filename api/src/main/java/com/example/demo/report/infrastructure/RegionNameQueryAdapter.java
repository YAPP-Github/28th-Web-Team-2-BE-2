package com.example.demo.report.infrastructure;

import com.example.demo.report.application.port.RegionNameQueryPort;
import com.example.demo.user.application.port.RegionReferenceRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RegionNameQueryAdapter implements RegionNameQueryPort {

    private final RegionReferenceRepository regionReferenceRepository;

    @Override
    public Optional<String> findName(final String regionId) {
        return Optional.ofNullable(regionReferenceRepository.findNamesByIds(List.of(regionId)).get(regionId));
    }
}
