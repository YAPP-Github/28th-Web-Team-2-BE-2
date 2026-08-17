package com.example.demo.item.infrastructure;

import com.example.demo.item.application.port.ItemFavoriteCommandPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ItemFavoriteCommandAdapter implements ItemFavoriteCommandPort {

    private final ItemJpaRepository itemJpaRepository;
    private final ItemFavoriteJpaRepository itemFavoriteJpaRepository;

    @Override
    public boolean itemExists(final Long itemId) {
        return itemJpaRepository.existsById(itemId);
    }

    @Override
    public void add(final Long userId, final Long itemId) {
        itemFavoriteJpaRepository.add(userId, itemId);
    }

    @Override
    public void delete(final Long userId, final Long itemId) {
        itemFavoriteJpaRepository.delete(userId, itemId);
    }
}
