package com.example.demo.item.presentation.dto;

import com.example.demo.item.application.query.PublicPricePeriod;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record ItemPublicPriceResponse(
        Long itemId,
        @Schema(nullable = true, description = "품목 기준 단위") String defaultUnit,
        PublicPricePeriod period,
        @Schema(description = "가격이 있는 날짜만 오름차순으로 담는다. 없는 날짜는 보간하지 않는다")
                List<PublicPricePointResponse> points) {}
