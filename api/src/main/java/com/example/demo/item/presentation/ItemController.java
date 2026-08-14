package com.example.demo.item.presentation;

import com.example.demo.common.presentation.ApiV1Response;
import com.example.demo.item.application.query.ItemQuery;
import com.example.demo.item.application.result.ItemQueryResult;
import com.example.demo.item.application.usecase.GetItemQueryUseCase;
import com.example.demo.item.presentation.converter.ItemResultConverter;
import com.example.demo.item.presentation.dto.ItemPageResponse;
import com.example.demo.item.presentation.dto.ItemQueryRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springdoc.core.annotations.ParameterObject;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/items")
@RequiredArgsConstructor
public class ItemController {

    private final GetItemQueryUseCase getItemQueryUseCase;
    private final ItemResultConverter itemResultConverter;

    @GetMapping
    @Operation(summary = "품목 목록과 공공가격을 조회한다")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "품목 목록 조회 성공"),
        @ApiResponse(responseCode = "400", description = "페이지 요청값이 올바르지 않다")
    })
    public ResponseEntity<ApiV1Response<ItemPageResponse>> getItems(
        @ParameterObject @ModelAttribute final ItemQueryRequest request) {
        final ItemQueryResult result = getItemQueryUseCase.execute(
                new ItemQuery(request.regionId(), request.page(), request.size()));
        final ItemPageResponse data = itemResultConverter.toResponse(result);
        return ResponseEntity.ok(new ApiV1Response<>("SUCCESS", "품목 조회 성공", data));
    }
}
