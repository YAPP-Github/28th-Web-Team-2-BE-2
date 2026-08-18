package com.example.demo.item.application.port;

import com.example.demo.item.application.query.ItemQuery;
import com.example.demo.item.domain.Item;
import com.example.demo.item.domain.ItemCategory;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.Page;

public interface ItemQueryPort {

    Page<Item> findAll(ItemQuery query, Long userId);

    Map<ItemCategory, Long> countByCategory();

    Set<Long> findFavoriteItemIds(Long userId, List<Long> itemIds);
}
