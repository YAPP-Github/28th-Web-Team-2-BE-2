package com.example.demo.region.infrastructure;

import com.example.demo.region.domain.Region;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegionJpaRepository extends JpaRepository<Region, String> {}
