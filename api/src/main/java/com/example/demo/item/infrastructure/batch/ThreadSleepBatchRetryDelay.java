package com.example.demo.item.infrastructure.batch;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import com.example.demo.item.application.port.BatchRetryDelayPort;
import java.time.Duration;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class ThreadSleepBatchRetryDelay implements BatchRetryDelayPort {

    @Override
    public void delay(final Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ApiException(
                    ErrorType.UNKNOWN_ERROR.description(),
                    ErrorType.UNKNOWN_ERROR,
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    exception);
        }
    }
}
