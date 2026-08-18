package com.example.demo.item.application.port;

import com.example.demo.item.domain.Item;
import java.util.Optional;

public interface ItemExistencePort {

    Optional<Item> findById(Long itemId);
}
