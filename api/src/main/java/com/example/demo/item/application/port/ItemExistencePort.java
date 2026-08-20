package com.example.demo.item.application.port;

import com.example.demo.item.domain.Item;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public interface ItemExistencePort {

    Optional<Item> findById(Long itemId);

    /** 품목 ID별 품목명이다. 없는 품목은 결과에 담기지 않는다. */
    Map<Long, String> findNamesByIds(Collection<Long> itemIds);
}
