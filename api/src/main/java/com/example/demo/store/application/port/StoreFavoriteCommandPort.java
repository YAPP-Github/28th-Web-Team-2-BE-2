package com.example.demo.store.application.port;

public interface StoreFavoriteCommandPort {

    boolean storeExists(Long storeId);

    void add(Long userId, Long storeId);

    void remove(Long userId, Long storeId);
}
