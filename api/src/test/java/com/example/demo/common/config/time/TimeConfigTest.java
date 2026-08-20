package com.example.demo.common.config.time;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TimeConfigTest {

    /**
     * 서비스 기준 시간대를 못박는다.
     *
     * <p>날짜 경계를 다루는 테스트들은 고정 시계로 이 빈을 대체하므로 프로덕션 시간대를 검증하지 못한다. 그 자리를 이 테스트가 막는다.
     */
    @Test
    @DisplayName("서비스 기준 시계의 시간대는 Asia/Seoul이다")
    void usesSeoulZone() {
        assertThat(new TimeConfig().serviceClock().getZone())
                .isEqualTo(ZoneId.of("Asia/Seoul"));
    }
}
