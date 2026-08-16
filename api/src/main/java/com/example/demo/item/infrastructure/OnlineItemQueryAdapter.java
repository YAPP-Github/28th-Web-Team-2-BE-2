package com.example.demo.item.infrastructure;

import com.example.demo.item.application.port.OnlineItemQueryPort;
import com.example.demo.item.domain.Item;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class OnlineItemQueryAdapter implements OnlineItemQueryPort {

    private final ItemJpaRepository itemJpaRepository;

    @Override
    public List<Item> findAll() {
        return itemJpaRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
    }
}
