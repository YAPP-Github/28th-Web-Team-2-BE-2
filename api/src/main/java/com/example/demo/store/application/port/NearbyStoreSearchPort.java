package com.example.demo.store.application.port;

import com.example.demo.store.application.query.NearbyStoreQuery;
import com.example.demo.store.application.result.NearbyStoreSearchResult;

public interface NearbyStoreSearchPort {

    NearbyStoreSearchResult search(NearbyStoreQuery query);
}
