package com.example.demo.item.infrastructure;

import com.example.demo.item.application.port.ItemExistencePort;
import com.example.demo.item.domain.Item;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ItemExistenceAdapter implements ItemExistencePort {

    private final ItemJpaRepository itemJpaRepository;

    @Override
    public Optional<Item> findById(final Long itemId) {
        return itemJpaRepository.findById(itemId);
    }

    @Override
    public Map<Long, String> findNamesByIds(final Collection<Long> itemIds) {
        if (itemIds.isEmpty()) {
            return Map.of();
        }
        return itemJpaRepository.findAllById(itemIds).stream()
                .collect(Collectors.toMap(Item::id, Item::name));
    }
}
