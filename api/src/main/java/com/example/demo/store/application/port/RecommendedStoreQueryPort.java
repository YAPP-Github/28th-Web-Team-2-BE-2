package com.example.demo.store.application.port;

import com.example.demo.store.application.result.RecommendedStoreSource;
import java.util.List;

public interface RecommendedStoreQueryPort {

    List<RecommendedStoreSource> findLatestCheapReports(String regionId);
}
