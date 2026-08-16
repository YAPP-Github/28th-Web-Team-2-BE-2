package com.example.demo.item.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.item.domain.OnlineChannel;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class OnlineChannelQueryAdapterTest {

    private final OnlineChannelJpaRepository onlineChannelJpaRepository;
    private final OnlineChannelQueryAdapter onlineChannelQueryAdapter;

    @Autowired
    OnlineChannelQueryAdapterTest(
            final OnlineChannelJpaRepository onlineChannelJpaRepository,
            final OnlineChannelQueryAdapter onlineChannelQueryAdapter) {
        this.onlineChannelJpaRepository = onlineChannelJpaRepository;
        this.onlineChannelQueryAdapter = onlineChannelQueryAdapter;
    }

    @BeforeEach
    void setUp() {
        onlineChannelJpaRepository.deleteAll();
    }

    @Test
    void 온라인_채널을_ID_오름차순으로_조회한다() {
        onlineChannelJpaRepository.saveAll(List.of(
                new OnlineChannel("오아시스"),
                new OnlineChannel("컬리")));

        final List<OnlineChannel> channels = onlineChannelQueryAdapter.findAll();

        assertThat(channels).extracting(OnlineChannel::name)
                .containsExactly("오아시스", "컬리");
    }
}
