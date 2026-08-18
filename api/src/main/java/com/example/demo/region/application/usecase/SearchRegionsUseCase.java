package com.example.demo.region.application.usecase;

import com.example.demo.region.application.port.RegionSearchQueryPort;
import com.example.demo.region.application.query.RegionSearchQuery;
import com.example.demo.region.application.result.RegionSearchResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SearchRegionsUseCase {

    private final RegionSearchQueryPort regionSearchQueryPort;

    public RegionSearchResult execute(final RegionSearchQuery query) {
        return regionSearchQueryPort.search(query);
    }
}
