package com.example.demo.store.infrastructure.persistence;

import com.example.demo.report.domain.Store;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StoreJpaRepository extends JpaRepository<Store, Long> {

    Optional<Store> findByKakaoPlaceId(String kakaoPlaceId);

    @Modifying
    @Query("""
            UPDATE Store store
               SET store.storeImageUrl = COALESCE(:storeImageUrl, store.storeImageUrl),
                   store.businessHours = COALESCE(:businessHours, store.businessHours),
                   store.openStatus = COALESCE(:openStatus, store.openStatus)
             WHERE store.id = :storeId
            """)
    void updateDetailFields(
            @Param("storeId") Long storeId,
            @Param("storeImageUrl") String storeImageUrl,
            @Param("businessHours") String businessHours,
            @Param("openStatus") String openStatus);
}
