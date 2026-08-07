package com.example.demo.price.infrastructure.batch;

import com.example.demo.price.application.port.CollectionTaskProvider;
import com.example.demo.price.application.command.CollectionTask;
import java.time.LocalDate;
import java.util.Iterator;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.infrastructure.item.ItemReader;

@RequiredArgsConstructor
public class CollectionTaskReader implements ItemReader<CollectionTask> {

    private final CollectionTaskProvider taskProvider;
    private final LocalDate priceDate;
    private final Long executionId;
    private Iterator<CollectionTask> tasks;

    @Override
    public CollectionTask read() {
        if (tasks == null) {
            tasks = taskProvider.activeTasks(priceDate, executionId).iterator();
        }
        if (!tasks.hasNext()) {
            return null;
        }
        return tasks.next();
    }
}
