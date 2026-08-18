package com.example.demo.item.infrastructure;

import com.example.demo.item.application.port.ItemExistencePort;
import com.example.demo.item.domain.Item;
import java.util.Optional;
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
}
