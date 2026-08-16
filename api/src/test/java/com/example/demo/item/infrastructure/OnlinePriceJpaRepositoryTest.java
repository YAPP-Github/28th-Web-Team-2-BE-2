package com.example.demo.item.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.item.domain.Item;
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

    @Test
    void 품목과_채널과_수집일의_100g당_최저가를_조회한다() {
        onlinePriceJpaRepository.saveAll(List.of(
                price("비싼 상품", PRICE_DATE, 500, 100),
                price("저렴한 상품", PRICE_DATE, 300, 100),
                price("다른 단위 상품", PRICE_DATE, 1, 1)));

        final Optional<OnlinePrice> lowestPrice = onlinePriceJpaRepository
                .findFirstByItemIdAndChannelIdAndCreatedAtAndUnitOrderByPriceAscIdAsc(
                        itemId, channelId, PRICE_DATE, 100);

        assertThat(lowestPrice).isPresent();
        assertThat(lowestPrice.orElseThrow().productName()).isEqualTo("저렴한 상품");
    }

    @Test
    void 같은_가격과_단위이면_먼저_저장된_상품을_조회한다() {
        onlinePriceJpaRepository.saveAll(List.of(
                price("먼저 저장된 상품", PRICE_DATE, 300, 100),
                price("나중에 저장된 상품", PRICE_DATE, 300, 100)));

        final Optional<OnlinePrice> lowestPrice = onlinePriceJpaRepository
                .findFirstByItemIdAndChannelIdAndCreatedAtAndUnitOrderByPriceAscIdAsc(
                        itemId, channelId, PRICE_DATE, 100);

        assertThat(lowestPrice).isPresent();
        assertThat(lowestPrice.orElseThrow().productName()).isEqualTo("먼저 저장된 상품");
    }

    private OnlinePrice price(final String productName, final LocalDate createdAt) {
        return price(productName, createdAt, 890, 100);
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
