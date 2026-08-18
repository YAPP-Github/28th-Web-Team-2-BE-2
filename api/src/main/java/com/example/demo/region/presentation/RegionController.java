package com.example.demo.region.presentation;

import com.example.demo.region.application.usecase.GetNearbyRegionUseCase;
import com.example.demo.region.application.usecase.SearchRegionsUseCase;
import com.example.demo.region.presentation.converter.RegionQueryConverter;
import com.example.demo.region.presentation.converter.RegionResultConverter;
import com.example.demo.region.presentation.dto.NearbyRegionRequest;
import com.example.demo.region.presentation.dto.NearbyRegionResponse;
import com.example.demo.region.presentation.dto.RegionSearchRequest;
import com.example.demo.region.presentation.dto.RegionSearchResponse;
import com.example.demo.region.presentation.spec.RegionControllerSpec;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/regions")
@RequiredArgsConstructor
public class RegionController implements RegionControllerSpec {

    private final GetNearbyRegionUseCase getNearbyRegionUseCase;
    private final SearchRegionsUseCase searchRegionsUseCase;
    private final RegionQueryConverter regionQueryConverter;
    private final RegionResultConverter regionResultConverter;

    @GetMapping("/nearby")
    @Override
    public ResponseEntity<List<NearbyRegionResponse>> getNearbyRegions(
            @Valid @ModelAttribute final NearbyRegionRequest request) {
        final List<NearbyRegionResponse> response = regionResultConverter.toNearbyRegionResponses(
                getNearbyRegionUseCase.execute(regionQueryConverter.toNearbyRegionQuery(request)));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    @Override
    public ResponseEntity<RegionSearchResponse> searchRegions(
            @Valid @ModelAttribute final RegionSearchRequest request) {
        final RegionSearchResponse data = regionResultConverter.toRegionSearchResponse(
                searchRegionsUseCase.execute(regionQueryConverter.toRegionSearchQuery(request)));
        return ResponseEntity.ok(data);
    }
}
