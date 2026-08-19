package com.example.demo.store.infrastructure.persistence;

import com.example.demo.store.application.port.StoreFavoriteCommandPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class StoreFavoriteCommandAdapter implements StoreFavoriteCommandPort {

    private final StoreJpaRepository storeJpaRepository;
    private final StoreFavoriteJpaRepository storeFavoriteJpaRepository;

    @Override
    public boolean storeExists(final Long storeId) {
        return storeJpaRepository.existsById(storeId);
    }

    @Override
    public void add(final Long userId, final Long storeId) {
        storeFavoriteJpaRepository.add(userId, storeId);
    }
}
