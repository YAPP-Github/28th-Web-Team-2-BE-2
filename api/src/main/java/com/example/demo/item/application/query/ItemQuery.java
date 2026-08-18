package com.example.demo.item.application.query;

import com.example.demo.item.domain.ItemCategory;

public record ItemQuery(
        String regionId,
        int page,
        int size,
        ItemSort sort,
        String keyword,
        ItemCategory category,
        boolean favoriteOnly) {

    public ItemQuery(
            final String regionId,
            final int page,
            final int size,
            final ItemSort sort) {
        this(regionId, page, size, sort, null, null, false);
    }

    public ItemQuery(
            final String regionId,
            final int page,
            final int size,
            final ItemSort sort,
            final String keyword) {
        this(regionId, page, size, sort, keyword, null, false);
    }

    public ItemQuery(
            final String regionId,
            final int page,
            final int size,
            final ItemSort sort,
            final String keyword,
            final boolean favoriteOnly) {
        this(regionId, page, size, sort, keyword, null, favoriteOnly);
    }

    public ItemQuery(final String regionId, final int page, final int size) {
        this(regionId, page, size, ItemSort.NAME_ASC, null, null, false);
    }
}
