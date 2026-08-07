package com.example.demo.kamis.presentation;

import com.example.demo.kamis.application.result.KamisDailyPriceResult;
import com.example.demo.kamis.application.usecase.GetKamisDailyPriceUseCase;
import com.example.demo.kamis.presentation.converter.KamisDailyPriceConverter;
import com.example.demo.kamis.presentation.dto.KamisDailyPriceRequest;
import com.example.demo.kamis.presentation.dto.KamisDailyPriceResponse;
import com.example.demo.kamis.presentation.spec.KamisControllerSpec;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/kamis")
@RequiredArgsConstructor
public class KamisController implements KamisControllerSpec {

    private final GetKamisDailyPriceUseCase getKamisDailyPriceUseCase;
    private final KamisDailyPriceConverter converter;

    @GetMapping("/daily-prices")
    @Override
    public ResponseEntity<KamisDailyPriceResponse> getDailyPrices(
            @Valid @ModelAttribute final KamisDailyPriceRequest request) {
        final KamisDailyPriceResult result = getKamisDailyPriceUseCase.execute(converter.toQuery(request));
        return ResponseEntity.ok(converter.toResponse(result));
    }
}
