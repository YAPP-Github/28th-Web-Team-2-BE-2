package com.example.demo.item.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.item.domain.Item;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class OnlineItemQueryAdapterTest {

    private final ItemJpaRepository itemJpaRepository;
    private final OnlineItemQueryAdapter onlineItemQueryAdapter;

    @Autowired
    OnlineItemQueryAdapterTest(
            final ItemJpaRepository itemJpaRepository,
            final OnlineItemQueryAdapter onlineItemQueryAdapter) {
        this.itemJpaRepository = itemJpaRepository;
        this.onlineItemQueryAdapter = onlineItemQueryAdapter;
    }

    @BeforeEach
    void setUp() {
        itemJpaRepository.deleteAll();
    }

    @Test
    void 품목을_ID_오름차순으로_조회한다() {
        itemJpaRepository.saveAll(List.of(
                new Item("감자", "1kg"),
                new Item("양파", "1kg")));

        final List<Item> items = onlineItemQueryAdapter.findAll();

        assertThat(items).extracting(Item::name)
                .containsExactly("감자", "양파");
    }
}
