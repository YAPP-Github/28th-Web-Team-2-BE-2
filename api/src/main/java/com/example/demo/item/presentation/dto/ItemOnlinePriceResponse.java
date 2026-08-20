package com.example.demo.item.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record ItemOnlinePriceResponse(
        Long itemId,
        @Schema(description = "최신 수집 회차의 채널별 최저가. 수집 데이터가 없으면 빈 목록이다")
                List<OnlineChannelPriceResponse> onlinePrices) {}
