package com.example.demo.item.infrastructure;

import com.example.demo.item.domain.ItemFavorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ItemFavoriteJpaRepository extends JpaRepository<ItemFavorite, Long> {

    @Modifying
    @Query(
            value = """
                    INSERT INTO item_favorites (user_id, item_id)
                    VALUES (:userId, :itemId)
                    ON CONFLICT (user_id, item_id) DO NOTHING
                    """,
            nativeQuery = true)
    void add(@Param("userId") Long userId, @Param("itemId") Long itemId);

    @Modifying
    @Query(
            value = "DELETE FROM item_favorites WHERE user_id = :userId AND item_id = :itemId",
            nativeQuery = true)
    void delete(@Param("userId") Long userId, @Param("itemId") Long itemId);
}
