package com.example.demo.item.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.item.domain.Item;
import com.example.demo.item.domain.ItemCategory;
import com.example.demo.item.domain.OnlineChannel;
import com.example.demo.item.domain.OnlinePrice;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class OnlinePricePersistenceAdapterTest {

    private static final LocalDate PRICE_DATE = LocalDate.of(2026, 8, 15);

    private final ItemJpaRepository itemJpaRepository;
    private final OnlineChannelJpaRepository onlineChannelJpaRepository;
    private final OnlinePriceJpaRepository onlinePriceJpaRepository;
    private final OnlinePricePersistenceAdapter onlinePricePersistenceAdapter;

    private Long itemId;
    private Integer channelId;

    @Autowired
    OnlinePricePersistenceAdapterTest(
            final ItemJpaRepository itemJpaRepository,
            final OnlineChannelJpaRepository onlineChannelJpaRepository,
            final OnlinePriceJpaRepository onlinePriceJpaRepository,
            final OnlinePricePersistenceAdapter onlinePricePersistenceAdapter) {
        this.itemJpaRepository = itemJpaRepository;
        this.onlineChannelJpaRepository = onlineChannelJpaRepository;
        this.onlinePriceJpaRepository = onlinePriceJpaRepository;
        this.onlinePricePersistenceAdapter = onlinePricePersistenceAdapter;
    }

    @BeforeEach
    void setUp() {
        onlinePriceJpaRepository.deleteAll();
        onlineChannelJpaRepository.deleteAll();
        itemJpaRepository.deleteAll();
        itemId = itemJpaRepository.save(
                new Item("감자", "1kg", null, ItemCategory.ROOT_VEGETABLES)).id();
        channelId = onlineChannelJpaRepository.save(new OnlineChannel("컬리")).id();
    }

    @Test
    void 온라인_가격을_저장한다() {
        final OnlinePrice price = price("상품 A", PRICE_DATE, 300, 100);

        onlinePricePersistenceAdapter.saveAll(List.of(price));

        assertThat(onlinePriceJpaRepository.findAll()).extracting(OnlinePrice::productName)
                .containsExactly("상품 A");
    }

    @Test
    void 품목과_채널과_수집일이_같은_온라인_가격을_삭제한다() {
        onlinePriceJpaRepository.saveAll(List.of(
                price("상품 A", PRICE_DATE, 300, 100),
                price("지난 상품", PRICE_DATE.minusDays(1), 400, 100)));

        onlinePricePersistenceAdapter.deleteAll(itemId, channelId, PRICE_DATE);

        assertThat(onlinePriceJpaRepository.findAll()).extracting(OnlinePrice::productName)
                .containsExactly("지난 상품");
    }

    @Test
    void 채널별_100g당_최저가를_조회한다() {
        onlinePriceJpaRepository.saveAll(List.of(
                price("비싼 상품", PRICE_DATE, 500, 100),
                price("저렴한 상품", PRICE_DATE, 300, 100),
                price("다른 단위 상품", PRICE_DATE, 1, 1)));

        final Optional<OnlinePrice> lowestPrice = onlinePricePersistenceAdapter.findLowestPrice(
                itemId, channelId, PRICE_DATE, 100);

        assertThat(lowestPrice).isPresent()
                .get()
                .extracting(OnlinePrice::productName)
                .isEqualTo("저렴한 상품");
    }

    @Test
    void 같은_가격과_단위이면_먼저_저장된_상품을_조회한다() {
        onlinePriceJpaRepository.saveAll(List.of(
                price("먼저 저장된 상품", PRICE_DATE, 300, 100),
                price("나중에 저장된 상품", PRICE_DATE, 300, 100)));

        final Optional<OnlinePrice> lowestPrice = onlinePricePersistenceAdapter.findLowestPrice(
                itemId, channelId, PRICE_DATE, 100);

        assertThat(lowestPrice).isPresent()
                .get()
                .extracting(OnlinePrice::productName)
                .isEqualTo("먼저 저장된 상품");
    }

    private OnlinePrice price(
            final String productName,
            final LocalDate createdAt,
            final int price,
            final int unit) {
        return new OnlinePrice(
                itemId,
                channelId,
                "감자",
                productName,
                price,
                unit,
                "https://example.com/" + productName,
                "무료배송",
                createdAt);
    }
}
