package com.example.demo.item.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.item.domain.Item;
import com.example.demo.item.domain.OnlineChannel;
import com.example.demo.item.domain.OnlinePrice;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class OnlinePriceJpaRepositoryTest {

    private static final LocalDate PRICE_DATE = LocalDate.of(2026, 8, 15);

    private final ItemJpaRepository itemJpaRepository;
    private final OnlineChannelJpaRepository onlineChannelJpaRepository;
    private final OnlinePriceJpaRepository onlinePriceJpaRepository;

    private Long itemId;
    private Integer channelId;

    @Autowired
    OnlinePriceJpaRepositoryTest(
            final ItemJpaRepository itemJpaRepository,
            final OnlineChannelJpaRepository onlineChannelJpaRepository,
            final OnlinePriceJpaRepository onlinePriceJpaRepository) {
        this.itemJpaRepository = itemJpaRepository;
        this.onlineChannelJpaRepository = onlineChannelJpaRepository;
        this.onlinePriceJpaRepository = onlinePriceJpaRepository;
    }

    @BeforeEach
    void setUp() {
        onlinePriceJpaRepository.deleteAll();
        onlineChannelJpaRepository.deleteAll();
        itemJpaRepository.deleteAll();
        itemId = itemJpaRepository.save(new Item("감자", "1kg")).id();
        channelId = onlineChannelJpaRepository.save(new OnlineChannel("컬리")).id().intValue();
    }

    @Test
    void 품목과_채널과_수집일로_온라인_가격을_조회한다() {
        onlinePriceJpaRepository.saveAll(List.of(
                price("상품 A", PRICE_DATE),
                price("상품 B", PRICE_DATE),
                price("지난 상품", PRICE_DATE.minusDays(1))));

        final List<OnlinePrice> prices = onlinePriceJpaRepository
                .findAllByItemIdAndChannelIdAndCreatedAtOrderByIdAsc(itemId, channelId, PRICE_DATE);

        assertThat(prices).extracting(OnlinePrice::productName)
                .containsExactly("상품 A", "상품 B");
    }

    @Test
    void 품목과_채널과_수집일로_온라인_가격을_삭제한다() {
        onlinePriceJpaRepository.saveAll(List.of(
                price("상품 A", PRICE_DATE),
                price("상품 B", PRICE_DATE),
                price("지난 상품", PRICE_DATE.minusDays(1))));

        final long deletedCount = onlinePriceJpaRepository
                .deleteAllByItemIdAndChannelIdAndCreatedAt(itemId, channelId, PRICE_DATE);

        assertThat(deletedCount).isEqualTo(2);
        assertThat(onlinePriceJpaRepository.findAll()).extracting(OnlinePrice::productName)
                .containsExactly("지난 상품");
    }

    private OnlinePrice price(final String productName, final LocalDate createdAt) {
        return new OnlinePrice(
                itemId,
                channelId,
                "감자",
                productName,
                890,
                100,
                "https://example.com/" + productName,
                "무료배송",
                createdAt);
    }
}
