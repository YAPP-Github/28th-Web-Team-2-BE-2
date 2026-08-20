package com.example.demo.common.config.time;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 서비스 기준 시각이다.
 *
 * <p>기준 시간대를 {@code Asia/Seoul}로 고정해 JVM 기본 시간대에 의존하지 않는다. 배포 컨테이너는 {@code TZ} 설정이 없어 UTC로
 * 동작하므로, 날짜 경계를 다루는 로직이 기본 시간대를 쓰면 KST 00:00~09:00에 하루가 어긋난다.
 *
 * <p>{@link Clock}으로 주입하는 이유는 테스트에서 시각을 고정할 수 있게 하려는 것이다. 시간대만 상수로 두면 테스트가 프로덕션과 같은 식으로
 * 기대값을 계산해 검증이 동어반복이 된다.
 */
@Configuration
public class TimeConfig {

    public static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

    @Bean
    Clock serviceClock() {
        return Clock.system(SERVICE_ZONE);
    }
}
