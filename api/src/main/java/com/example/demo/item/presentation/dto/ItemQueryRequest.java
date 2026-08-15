package com.example.demo.item.presentation.dto;

import com.example.demo.item.application.query.ItemSort;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record ItemQueryRequest(
        @NotBlank
        @Schema(description = "법정동 코드", example = "1121510100") String regionId,
        @Min(0)
        @Schema(description = "페이지 번호", example = "0", defaultValue = "0") Integer page,
        @Min(1)
        @Max(100)
        @Schema(description = "페이지 크기", example = "10", defaultValue = "10") Integer size,
        @Schema(
                        description = "정렬 방식",
                        defaultValue = "NAME_ASC")
                ItemSort sort) {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;

    public ItemQueryRequest {
        page = defaultValue(page, DEFAULT_PAGE);
        size = defaultValue(size, DEFAULT_SIZE);
        sort = defaultSort(sort);
    }

    public ItemQueryRequest(final String regionId, final Integer page, final Integer size) {
        this(regionId, page, size, ItemSort.NAME_ASC);
    }

    private static int defaultValue(final Integer value, final int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        return value;
    }

    private static ItemSort defaultSort(final ItemSort sort) {
        if (sort == null) {
            return ItemSort.NAME_ASC;
        }
        return sort;
    }

}
