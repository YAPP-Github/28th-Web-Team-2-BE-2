package com.example.demo.item.presentation.converter;

import com.example.demo.item.application.query.ItemDetailQuery;
import com.example.demo.item.application.query.ItemQuery;
import com.example.demo.item.presentation.dto.ItemDetailRequest;
import com.example.demo.item.application.query.ItemSort;
import com.example.demo.item.presentation.dto.ItemQueryRequest;
import com.example.demo.item.presentation.dto.ItemSearchRequest;
import org.springframework.stereotype.Component;

@Component
public class ItemQueryRequestConverter {

    public ItemDetailQuery toQuery(final Long itemId, final ItemDetailRequest request) {
        return new ItemDetailQuery(itemId, request.regionId());
    }

    public ItemQuery toQuery(final ItemQueryRequest request) {
        return new ItemQuery(
                request.regionId(),
                request.page(),
                request.size(),
                request.sort(),
                request.keyword(),
                request.category(),
                request.favoriteOnly(),
                null);
    }

    public ItemQuery toQuery(final ItemSearchRequest request) {
        return new ItemQuery(
                request.regionId(),
                0,
                request.limit(),
                ItemSort.NAME_ASC,
                request.keyword(),
                null,
                false,
                request.offset());
    }
}
