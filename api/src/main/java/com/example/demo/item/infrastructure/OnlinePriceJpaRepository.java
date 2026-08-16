package com.example.demo.item.infrastructure;

import com.example.demo.item.domain.OnlinePrice;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface OnlinePriceJpaRepository extends JpaRepository<OnlinePrice, Long> {

    List<OnlinePrice> findAllByItemIdAndChannelIdAndCreatedAtOrderByIdAsc(
            Long itemId, Integer channelId, LocalDate createdAt);

    Optional<OnlinePrice> findFirstByItemIdAndChannelIdAndCreatedAtAndUnitOrderByPriceAscIdAsc(
            Long itemId, Integer channelId, LocalDate createdAt, Integer unit);

    @Transactional
    long deleteAllByItemIdAndChannelIdAndCreatedAt(
            Long itemId, Integer channelId, LocalDate createdAt);
}
