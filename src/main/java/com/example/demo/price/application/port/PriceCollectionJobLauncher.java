package com.example.demo.price.application.port;

import java.time.LocalDate;

public interface PriceCollectionJobLauncher {

    void launch(LocalDate priceDate);
}
