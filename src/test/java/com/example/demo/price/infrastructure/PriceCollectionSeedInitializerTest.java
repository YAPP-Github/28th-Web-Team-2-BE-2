package com.example.demo.price.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.price.application.port.CollectionTaskProvider;
import com.example.demo.price.domain.ChannelCode;
import com.example.demo.price.domain.OnlineChannelEntity;
import com.example.demo.price.domain.PriceUnit;
import java.time.LocalDate;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class PriceCollectionSeedInitializerTest {

    @Autowired
    private ItemJpaRepository itemJpaRepository;

    @Autowired
    private OnlineChannelJpaRepository onlineChannelJpaRepository;

    @Autowired
    private CollectionTaskProvider collectionTaskProvider;

    @Test
    void 감자와_온라인_채널_seed가_등록된다() {
        assertThat(itemJpaRepository.findAllByActiveTrueOrderByIdAsc())
                .extracting(item -> item.getName())
                .contains("감자");

        final Map<ChannelCode, OnlineChannelEntity> channels = onlineChannelJpaRepository.findAll().stream()
                .collect(Collectors.toMap(OnlineChannelEntity::getCode, Function.identity()));

        assertThat(channels).containsKeys(ChannelCode.OASIS, ChannelCode.KURLY,
                ChannelCode.ELEVEN_ST, ChannelCode.GS_SHOP);
        assertThat(channels.get(ChannelCode.OASIS).isActive()).isTrue();
        assertThat(channels.get(ChannelCode.KURLY).isActive()).isFalse();
        assertThat(channels.get(ChannelCode.ELEVEN_ST).isActive()).isFalse();
        assertThat(channels.get(ChannelCode.GS_SHOP).isActive()).isFalse();
    }

    @Test
    void 오아시스_수집_task는_확정된_46개_품목과_비교_단위를_사용한다() {
        final Map<String, PriceUnit> targetUnits = collectionTaskProvider
                .activeTasks(LocalDate.of(2026, 8, 7), 1L).stream()
                .collect(Collectors.toMap(task -> task.itemName(), task -> task.targetUnit()));

        assertThat(targetUnits).hasSize(46);
        assertThat(targetUnits).containsEntry("감자", PriceUnit.KG);
        assertThat(targetUnits).containsEntry("배추", PriceUnit.COUNT);
        assertThat(targetUnits).containsEntry("무", PriceUnit.COUNT);
        assertThat(targetUnits).containsEntry("느타리버섯", PriceUnit.G);
        assertThat(targetUnits).containsEntry("고춧가루-국산", PriceUnit.KG);
        assertThat(targetUnits).containsEntry("딸기", PriceUnit.G);
    }
}
