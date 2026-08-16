package com.example.demo.item.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.item.domain.Item;
import com.example.demo.item.domain.OnlineChannel;
import com.example.demo.item.domain.OnlinePrice;
import com.example.demo.item.infrastructure.ItemJpaRepository;
import com.example.demo.item.infrastructure.OnlineChannelJpaRepository;
import com.example.demo.item.infrastructure.OnlinePriceJpaRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ReplaceOnlinePriceUseCaseIntegrationTest {

    private static final LocalDate COLLECTION_DATE = LocalDate.of(2026, 8, 16);
    private static final String PRODUCT_URL = "https://example.com/same-product";

    @Autowired
    private ReplaceOnlinePriceUseCase useCase;

    @Autowired
    private ItemJpaRepository itemJpaRepository;

    @Autowired
    private OnlineChannelJpaRepository onlineChannelJpaRepository;

    @Autowired
    private OnlinePriceJpaRepository onlinePriceJpaRepository;

    private Long itemId;
    private Integer channelId;

    @BeforeEach
    void setUp() {
        onlinePriceJpaRepository.deleteAll();
        onlineChannelJpaRepository.deleteAll();
        itemJpaRepository.deleteAll();
        itemId = itemJpaRepository.save(new Item("감자", "1kg")).id();
        channelId = onlineChannelJpaRepository.save(new OnlineChannel("컬리")).id();
    }

    @Test
    void 같은_날짜와_URL의_기존_가격을_새_가격으로_교체한다() {
        onlinePriceJpaRepository.save(price("기존 상품", 300));

        useCase.execute(itemId, channelId, COLLECTION_DATE, List.of(price("새 상품", 200)));

        assertThat(onlinePriceJpaRepository.findAll()).singleElement()
                .satisfies(savedPrice -> {
                    assertThat(savedPrice.productName()).isEqualTo("새 상품");
                    assertThat(savedPrice.price()).isEqualTo(200);
                    assertThat(savedPrice.productUrl()).isEqualTo(PRODUCT_URL);
                });
    }

    private OnlinePrice price(final String productName, final int price) {
        return new OnlinePrice(
                itemId,
                channelId,
                "감자",
                productName,
                price,
                100,
                PRODUCT_URL,
                "무료배송",
                COLLECTION_DATE);
    }
}
