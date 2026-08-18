package com.example.demo.item.application.port;

import java.time.Duration;

public interface BatchRetryDelayPort {

    void delay(Duration duration);
}
