package com.example.demo.item.infrastructure;

import com.example.demo.item.application.port.ItemQueryPort;
import com.example.demo.item.domain.Item;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ItemQueryAdapter implements ItemQueryPort {

    private final ItemJpaRepository itemJpaRepository;

    @Override
    public Page<Item> findAll(final Pageable pageable) {
        return itemJpaRepository.findAll(pageable);
    }
}
