package com.example.demo.item.infrastructure;

import com.example.demo.item.domain.OnlinePrice;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface OnlinePriceJpaRepository extends JpaRepository<OnlinePrice, Long> {

    List<OnlinePrice> findAllByItemIdAndChannelIdAndCreatedAtOrderByIdAsc(
            Long itemId, Integer channelId, LocalDate createdAt);

    Optional<OnlinePrice> findFirstByItemIdAndChannelIdAndCreatedAtAndUnitOrderByPriceAscIdAsc(
            Long itemId, Integer channelId, LocalDate createdAt, Integer unit);

    Optional<OnlinePrice> findFirstByItemIdOrderByCreatedAtDescIdDesc(Long itemId);

    Optional<OnlinePrice> findFirstByItemIdAndCreatedAtAndUnitOrderByPriceAscIdAsc(
            Long itemId, LocalDate createdAt, Integer unit);

    @Transactional
    @Modifying
    @Query("""
            delete from OnlinePrice price
            where price.itemId = ?1
              and price.channelId = ?2
              and price.createdAt = ?3
            """)
    int deleteAllByItemIdAndChannelIdAndCreatedAt(
            Long itemId, Integer channelId, LocalDate createdAt);
}
