package com.example.demo.report.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.common.exception.GlobalExceptionHandler;
import com.example.demo.common.presentation.ResponseWrapper;
import com.example.demo.report.application.result.StoreReportsResult;
import com.example.demo.report.application.result.StoreReportResult;
import com.example.demo.report.application.result.PriceClassification;
import com.example.demo.report.application.usecase.GetRegionLowestPriceReportsUseCase;
import com.example.demo.report.application.usecase.GetStoreReportsUseCase;
import com.example.demo.report.presentation.converter.RegionLowestPriceQueryConverter;
import com.example.demo.report.presentation.converter.RegionLowestPriceResultConverter;
import com.example.demo.report.presentation.converter.UserReportQueryConverter;
import com.example.demo.report.presentation.converter.UserReportResultConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class StoreReportControllerTest {

    private final GetStoreReportsUseCase getStoreReportsUseCase = mock(GetStoreReportsUseCase.class);
    private final GetRegionLowestPriceReportsUseCase getRegionLowestPriceReportsUseCase =
            mock(GetRegionLowestPriceReportsUseCase.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        final UserReportController controller = new UserReportController(
                mock(com.example.demo.report.application.usecase.CreateUserReportUseCase.class),
                getStoreReportsUseCase,
                getRegionLowestPriceReportsUseCase,
                new com.example.demo.report.presentation.converter.UserReportCommandConverter(),
                new UserReportQueryConverter(),
                new UserReportResultConverter(),
                new RegionLowestPriceQueryConverter(),
                new RegionLowestPriceResultConverter());
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler(), new ResponseWrapper())
                .build();
    }

    @Test
    void 공개_가게_제보_조회는_필터와_페이지_응답을_반환한다() throws Exception {
        when(getStoreReportsUseCase.execute(any()))
                .thenReturn(StoreReportsResultFixture.cheapAndExpensive());

        mockMvc.perform(get("/api/v1/stores/7/reports")
                        .queryParam("filter", "CHEAP")
                        .queryParam("page", "1")
                        .queryParam("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.storeId").value(7))
                .andExpect(jsonPath("$.data.summary.cheapCount").value(1))
                .andExpect(jsonPath("$.data.summary.expensiveCount").value(1))
                .andExpect(jsonPath("$.data.reports[0].priceClassification").value("CHEAP"))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(2))
                .andExpect(jsonPath("$.data.hasNext").value(true));
    }

    @Test
    void filter가_잘못되면_400을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/stores/7/reports")
                        .queryParam("filter", "EQUAL"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER_ERROR"));
    }

    @Test
    void ALL_filter는_전체_목록을_요청할_수_있다() throws Exception {
        when(getStoreReportsUseCase.execute(any()))
                .thenReturn(StoreReportsResultFixture.cheapAndExpensive());

        mockMvc.perform(get("/api/v1/stores/7/reports")
                        .queryParam("filter", "ALL"))
                .andExpect(status().isOk());
    }

    @Test
    void 페이지_파라미터가_범위를_벗어나면_400을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/stores/7/reports")
                        .queryParam("page", "-1")
                        .queryParam("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER_ERROR"));
    }

    private static final class StoreReportsResultFixture {

        private static StoreReportsResult cheapAndExpensive() {
            return new StoreReportsResult(
                    7L,
                    1L,
                    1L,
                    java.util.List.of(new StoreReportResult(
                            11L, 3L, "감자", "https://image.example.com/potato.jpg", 900,
                            "1kg", java.time.LocalDate.of(2026, 8, 20), -100,
                            new java.math.BigDecimal("-10.00"), PriceClassification.CHEAP)),
                    1,
                    2,
                    true);
        }
    }
}
