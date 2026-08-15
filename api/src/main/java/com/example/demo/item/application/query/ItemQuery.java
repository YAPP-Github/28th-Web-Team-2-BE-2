package com.example.demo.item.application.query;

public record ItemQuery(String regionId, int page, int size, ItemSort sort) {

    public ItemQuery(final String regionId, final int page, final int size) {
        this(regionId, page, size, ItemSort.NAME_ASC);
    }
}
