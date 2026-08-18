package com.example.demo.store.infrastructure.persistence;

import com.example.demo.report.domain.Store;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreJpaRepository extends JpaRepository<Store, Long> {

    Optional<Store> findByKakaoPlaceId(String kakaoPlaceId);
}
