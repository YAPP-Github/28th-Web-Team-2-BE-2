package com.example.demo.item.application.query;

public record ItemQuery(
        String regionId,
        int page,
        int size,
        ItemSort sort,
        String keyword,
        boolean favoriteOnly) {

    public ItemQuery(
            final String regionId,
            final int page,
            final int size,
            final ItemSort sort) {
        this(regionId, page, size, sort, null, false);
    }

    public ItemQuery(
            final String regionId,
            final int page,
            final int size,
            final ItemSort sort,
            final String keyword) {
        this(regionId, page, size, sort, keyword, false);
    }

    public ItemQuery(final String regionId, final int page, final int size) {
        this(regionId, page, size, ItemSort.NAME_ASC);
    }
}
