package com.example.demo.price.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

class PriceEntityTest {

    @Test
    void 활성_품목을_생성하면_품목_정보를_보존한다() {
        final ItemEntity item = new ItemEntity(152, "감자", PriceUnit.KG, true);

        assertThat(item.getItemCode()).isEqualTo(152);
        assertThat(item.getName()).isEqualTo("감자");
        assertThat(item.getTargetUnit()).isEqualTo(PriceUnit.KG);
        assertThat(item.isActive()).isTrue();
    }

    @Test
    void 온라인_채널을_생성하면_채널_정보를_보존한다() {
        final OnlineChannelEntity channel = new OnlineChannelEntity(
                1, ChannelCode.OASIS, "오아시스", true);

        assertThat(channel.getId()).isEqualTo(1);
        assertThat(channel.getCode()).isEqualTo(ChannelCode.OASIS);
        assertThat(channel.getName()).isEqualTo("오아시스");
        assertThat(channel.isActive()).isTrue();
    }

    @Test
    void 수집_실행을_생성하면_실행_결과를_보존한다() {
        final OffsetDateTime startedAt = OffsetDateTime.now();
        final OffsetDateTime finishedAt = startedAt.plusMinutes(1);
        final CollectionExecutionEntity execution = new CollectionExecutionEntity(
                10L, 1L, "감자", "OASIS", CollectionStatus.SUCCEEDED,
                startedAt, finishedAt, 3, null);

        assertThat(execution.getExecutionId()).isEqualTo(10L);
        assertThat(execution.getItemId()).isEqualTo(1L);
        assertThat(execution.getItemName()).isEqualTo("감자");
        assertThat(execution.getChannel()).isEqualTo("OASIS");
        assertThat(execution.getStatus()).isEqualTo(CollectionStatus.SUCCEEDED);
        assertThat(execution.getStartedAt()).isEqualTo(startedAt);
        assertThat(execution.getFinishedAt()).isEqualTo(finishedAt);
        assertThat(execution.getValidOfferCount()).isEqualTo(3);
    }

    @Test
    void 수집_실패_사유가_컬럼_길이를_초과하면_저장_가능한_길이로_제한한다() {
        final CollectionExecutionEntity execution = new CollectionExecutionEntity(
                10L, 1L, "감자", "OASIS", CollectionStatus.FAILED,
                OffsetDateTime.now(), OffsetDateTime.now(), 0, "x".repeat(1_500));

        assertThat(execution.getFailureReason()).hasSize(1_000);
    }
}
