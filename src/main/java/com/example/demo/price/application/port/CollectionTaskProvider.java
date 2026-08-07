package com.example.demo.price.application.port;

import com.example.demo.price.application.command.CollectionTask;
import java.time.LocalDate;
import java.util.List;

public interface CollectionTaskProvider {

    List<CollectionTask> activeTasks(LocalDate priceDate, Long executionId);
}
