package com.example.demo.item.application.port;

public interface ItemFavoriteCommandPort {

    boolean itemExists(Long itemId);

    void add(Long userId, Long itemId);

    void delete(Long userId, Long itemId);
}
