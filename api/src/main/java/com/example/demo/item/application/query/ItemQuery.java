package com.example.demo.item.application.query;

import com.example.demo.item.domain.ItemCategory;

/**
 * 품목 조회 조건이다.
 *
 * <p>{@code offset}은 limit·offset 계약을 쓰는 호출자의 조회 시작 위치다. {@code null}이면 {@code page * size}를 쓴다.
 * 조회 시작 위치가 필요한 곳은 {@link #effectiveOffset()}만 사용한다.
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

    /** 조회 시작 위치다. offset 계약이면 그 값을, page 계약이면 {@code page * size}를 쓴다. */
    public long effectiveOffset() {
        if (offset != null) {
            return offset;
        }
        return (long) page * size;
    }
}
