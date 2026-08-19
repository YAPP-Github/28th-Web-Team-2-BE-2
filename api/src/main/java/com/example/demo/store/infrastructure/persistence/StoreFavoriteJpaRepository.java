package com.example.demo.store.infrastructure.persistence;

import com.example.demo.store.domain.StoreFavorite;
import java.util.Collection;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StoreFavoriteJpaRepository extends JpaRepository<StoreFavorite, Long> {

    @Modifying
    @Query(
            value = """
                    INSERT INTO store_favorites (user_id, store_id)
                    VALUES (:userId, :storeId)
                    ON CONFLICT (user_id, store_id) DO NOTHING
                    """,
            nativeQuery = true)
    void add(@Param("userId") Long userId, @Param("storeId") Long storeId);
    @Query("""
            select favorite.storeId
            from StoreFavorite favorite
            where favorite.userId = :userId
              and favorite.storeId in :storeIds
            """)
    Set<Long> findStoreIdsByUserIdAndStoreIdIn(
            @Param("userId") Long userId,
            @Param("storeIds") Collection<Long> storeIds);
}
