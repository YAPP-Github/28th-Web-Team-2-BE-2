package com.example.demo.store.application.port;

import com.example.demo.store.application.result.StoreDetailSnapshot;

public interface StoreDetailEnrichmentPort {

    StoreDetailSnapshot enrich(StoreDetailSnapshot snapshot);
}
