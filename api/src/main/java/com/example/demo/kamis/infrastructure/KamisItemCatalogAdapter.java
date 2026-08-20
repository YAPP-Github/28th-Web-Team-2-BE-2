package com.example.demo.kamis.infrastructure;

import com.example.demo.item.application.port.OnlineItemQueryPort;
import com.example.demo.kamis.application.port.KamisItemCatalogPort;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class KamisItemCatalogAdapter implements KamisItemCatalogPort {

    private final OnlineItemQueryPort onlineItemQueryPort;

    @Override
    public List<KamisItem> findAll() {
        return onlineItemQueryPort.findAll().stream()
                .map(item -> new KamisItem(item.id(), item.name(), item.defaultUnit()))
                .toList();
    }
}
