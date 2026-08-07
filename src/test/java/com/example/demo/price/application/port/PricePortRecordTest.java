package com.example.demo.price.application.port;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.price.domain.ChannelCode;
import com.example.demo.price.domain.CollectionStatus;
import com.example.demo.price.domain.NormalizedPrice;
import com.example.demo.price.domain.PriceUnit;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

class PricePortRecordTest {

    @Test
    void 가격_저장_계약은_상품과_정규화_정보를_보존한다() {
        final var price = new OnlinePriceRepository.DailyProductPrice(
                1L, ChannelCode.OASIS, "감자", "국내산 감자 1kg", "url",
                new NormalizedPrice(BigDecimal.TEN, PriceUnit.KG), LocalDate.now());

        assertThat(price.itemName()).isEqualTo("감자");
        assertThat(price.price().unit()).isEqualTo(PriceUnit.KG);
    }

    @Test
    void 실행_기록_계약은_상태와_실패_사유를_보존한다() {
        final var execution = new CollectionExecutionRepository.TaskExecution(
                1L, 1L, "감자", "OASIS", CollectionStatus.BLOCKED,
                OffsetDateTime.now(), OffsetDateTime.now(), 0, "CAPTCHA");

        assertThat(execution.status()).isEqualTo(CollectionStatus.BLOCKED);
        assertThat(execution.failureReason()).isEqualTo("CAPTCHA");
    }
}
