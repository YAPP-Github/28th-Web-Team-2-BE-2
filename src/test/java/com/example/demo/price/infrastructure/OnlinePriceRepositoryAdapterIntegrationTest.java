package com.example.demo.price.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.price.application.port.OnlinePriceRepository.DailyProductPrice;
import com.example.demo.price.domain.ChannelCode;
import com.example.demo.price.domain.NormalizedPrice;
import com.example.demo.price.domain.OnlinePriceEntity;
import com.example.demo.price.domain.PriceUnit;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class OnlinePriceRepositoryAdapterIntegrationTest {

    private static final LocalDate COLLECTION_DATE = LocalDate.of(2026, 8, 7);

    @Autowired
    private OnlinePriceRepositoryAdapter onlinePriceRepositoryAdapter;

    @Autowired
    private OnlinePriceJpaRepository onlinePriceJpaRepository;

    @BeforeEach
    void setUp() {
        onlinePriceJpaRepository.deleteAll();
    }

    @Test
    void 같은_품목_채널_상품_수집일은_재실행해도_한_row로_upsert된다() {
        onlinePriceRepositoryAdapter.upsert(dailyPrice(3_000, "https://example.com/first"));
        onlinePriceRepositoryAdapter.upsert(dailyPrice(3_500, "https://example.com/updated"));

        final OnlinePriceEntity saved = onlinePriceJpaRepository.findAll().stream()
                .findFirst()
                .orElseThrow();

        assertThat(onlinePriceJpaRepository.count()).isEqualTo(1);
        assertThat(saved.getItemId()).isEqualTo(1L);
        assertThat(saved.getChannel()).isEqualTo(ChannelCode.OASIS);
        assertThat(saved.getProductName()).isEqualTo("감자 1kg");
        assertThat(saved.getProductUrl()).isEqualTo("https://example.com/updated");
        assertThat(saved.getPrice()).isEqualTo(3_500);
        assertThat(saved.getPricePer100g()).isEqualTo(700);
        assertThat(saved.getCreatedAt()).isEqualTo(COLLECTION_DATE);
    }

    private DailyProductPrice dailyPrice(final int amount, final String productUrl) {
        return new DailyProductPrice(
                1L,
                ChannelCode.OASIS,
                "감자",
                "감자 1kg",
                productUrl,
                new NormalizedPrice(BigDecimal.valueOf(amount), PriceUnit.KG,
                        BigDecimal.valueOf(amount).divide(BigDecimal.valueOf(5), 2)),
                COLLECTION_DATE);
    }
}
