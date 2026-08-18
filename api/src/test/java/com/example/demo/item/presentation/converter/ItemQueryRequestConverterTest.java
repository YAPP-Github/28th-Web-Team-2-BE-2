package com.example.demo.item.presentation.converter;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.item.application.query.ItemQuery;
import com.example.demo.item.application.query.ItemSort;
import com.example.demo.item.domain.ItemCategory;
import com.example.demo.item.presentation.dto.ItemQueryRequest;
import org.junit.jupiter.api.Test;

class ItemQueryRequestConverterTest {

    @Test
    void 요청을_정렬과_검색조건을_포함한_품목조회쿼리로_변환한다() {
        final ItemQueryRequest request =
                new ItemQueryRequest("1121510100", 2, 20, ItemSort.PRICE_DESC, "  파  ");

        final ItemQuery result = new ItemQueryRequestConverter().toQuery(request);

        assertThat(result)
                .isEqualTo(new ItemQuery("1121510100", 2, 20, ItemSort.PRICE_DESC, "파"));
    }

    @Test
    void category를_품목조회쿼리로_변환한다() {
        final ItemQueryRequest request = new ItemQueryRequest(
                "1121510100", 0, 10, ItemSort.NAME_ASC, null, ItemCategory.ROOT_VEGETABLES, false);

        final ItemQuery result = new ItemQueryRequestConverter().toQuery(request);

        assertThat(result.category()).isEqualTo(ItemCategory.ROOT_VEGETABLES);
    }
}
