package com.example.demo.price.infrastructure;

import com.example.demo.price.application.port.OnlinePriceRepository;
import com.example.demo.price.domain.OnlinePriceEntity;
import java.math.RoundingMode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class OnlinePriceRepositoryAdapter implements OnlinePriceRepository {

    private final OnlinePriceJpaRepository repository;

    @Override
    public void upsert(final DailyProductPrice price) {
        final OnlinePriceEntity entity = repository
                .findByItemIdAndChannelAndProductNameAndCreatedAt(
                        price.itemId(), price.channel(), price.productName(), price.createdAt())
                .orElseGet(() -> new OnlinePriceEntity(
                        price.itemId(), price.channel(), price.itemName(), price.productName(),
                        price.productUrl(), toInteger(price.price().amount()),
                        nullableInteger(price.price().pricePer100g()),
                        price.price().unit().ordinal(), price.createdAt()));
        entity.update(price.itemName(), price.productName(), price.productUrl(),
                toInteger(price.price().amount()), nullableInteger(price.price().pricePer100g()),
                price.price().unit().ordinal());
        repository.save(entity);
    }

    private int toInteger(final java.math.BigDecimal amount) {
        return amount.setScale(0, RoundingMode.HALF_UP).intValueExact();
    }

    private Integer nullableInteger(final java.math.BigDecimal amount) {
        if (amount == null) {
            return null;
        }
        return toInteger(amount);
    }
}
