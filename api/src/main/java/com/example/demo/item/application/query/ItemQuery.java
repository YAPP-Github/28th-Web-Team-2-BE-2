package com.example.demo.item.application.query;

import com.example.demo.item.domain.ItemCategory;

/**
 * 품목 조회 조건이다.
 *
 * <p>{@code offset}은 limit·offset 계약을 쓰는 호출자를 위한 조회 시작 위치이며, {@code null}이면 {@code page * size}를
 * 사용한다.
 */
public record ItemQuery(
        String regionId,
        int page,
        int size,
        ItemSort sort,
        String keyword,
        ItemCategory category,
        boolean favoriteOnly,
        Integer offset) {

    public ItemQuery(
            final String regionId,
            final int page,
            final int size,
            final ItemSort sort) {
        this(regionId, page, size, sort, null, null, false, null);
    }

    public ItemQuery(
            final String regionId,
            final int page,
            final int size,
            final ItemSort sort,
            final String keyword) {
        this(regionId, page, size, sort, keyword, null, false, null);
    }

    public ItemQuery(
            final String regionId,
            final int page,
            final int size,
            final ItemSort sort,
            final String keyword,
            final boolean favoriteOnly) {
        this(regionId, page, size, sort, keyword, null, favoriteOnly, null);
    }

    public ItemQuery(final String regionId, final int page, final int size) {
        this(regionId, page, size, ItemSort.NAME_ASC, null, null, false, null);
    }
}
