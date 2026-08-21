package com.example.demo.store.application.port;

import com.example.demo.store.application.result.StoreDetailEnrichment;

public interface StoreDetailPersistencePort {

    void update(Long storeId, StoreDetailEnrichment enrichment);
}
