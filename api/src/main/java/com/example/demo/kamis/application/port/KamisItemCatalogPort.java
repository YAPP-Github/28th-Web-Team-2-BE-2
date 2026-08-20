package com.example.demo.kamis.application.port;

import java.util.List;

public interface KamisItemCatalogPort {

    List<KamisItem> findAll();

    record KamisItem(Long itemId, String itemName, String defaultUnit) {}
}
