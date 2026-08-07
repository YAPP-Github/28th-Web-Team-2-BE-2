package com.example.demo.price.infrastructure;

import com.example.demo.price.domain.ChannelCode;
import com.example.demo.price.domain.OnlinePriceEntity;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface OnlinePriceJpaRepository extends JpaRepository<OnlinePriceEntity, Long> {

    Optional<OnlinePriceEntity> findByItemIdAndChannelAndProductNameAndCreatedAt(
            Long itemId, ChannelCode channel, String productName, LocalDate createdAt);

    @Transactional
    void deleteAllByChannel(ChannelCode channel);
}
