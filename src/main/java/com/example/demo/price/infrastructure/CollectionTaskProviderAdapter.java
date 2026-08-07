package com.example.demo.price.infrastructure;

import com.example.demo.price.application.port.CollectionTaskProvider;
import com.example.demo.price.application.command.CollectionTask;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CollectionTaskProviderAdapter implements CollectionTaskProvider {

    private final ItemJpaRepository itemRepository;
    private final OnlineChannelJpaRepository channelRepository;

    @Override
    public List<CollectionTask> activeTasks(final LocalDate priceDate, final Long executionId) {
        final List<CollectionTask> tasks = new ArrayList<>();
        itemRepository.findAllByActiveTrueOrderByIdAsc().forEach(item ->
                channelRepository.findAllByActiveTrueOrderByIdAsc().forEach(channel -> tasks.add(
                        new CollectionTask(item.getId(), item.getName(), channel.getCode(), item.getTargetUnit(),
                                priceDate, executionId))));
        return tasks;
    }
}
