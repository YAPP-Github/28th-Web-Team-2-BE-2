package com.example.demo.item.presentation.converter;

import com.example.demo.item.application.query.ItemQuery;
import com.example.demo.item.presentation.dto.ItemQueryRequest;
import org.springframework.stereotype.Component;

@Component
public class ItemQueryRequestConverter {

    public ItemQuery toQuery(final ItemQueryRequest request) {
        return new ItemQuery(
                request.regionId(),
                request.page(),
                request.size(),
                request.sort(),
                request.keyword(),
                request.category(),
                request.favoriteOnly());
    }
}
