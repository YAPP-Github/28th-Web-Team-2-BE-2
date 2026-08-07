package com.example.demo.price.application.usecase;

import com.example.demo.price.application.port.PriceCollectionJobLauncher;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LaunchPriceCollectionUseCase {

    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

    private final PriceCollectionJobLauncher jobLauncher;

    public void execute(final LocalDate priceDate) {
        jobLauncher.launch(priceDate);
    }

    public void executeToday() {
        execute(LocalDate.now(KOREA_ZONE));
    }
}
