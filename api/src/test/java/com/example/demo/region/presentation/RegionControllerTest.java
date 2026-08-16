package com.example.demo.region.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.common.exception.ErrorType;
import com.example.demo.common.exception.GlobalExceptionHandler;
import com.example.demo.region.application.query.NearbyRegionQuery;
import com.example.demo.region.application.result.NearbyRegionResult;
import com.example.demo.region.application.usecase.GetNearbyRegionUseCase;
import com.example.demo.region.presentation.converter.RegionQueryConverter;
import com.example.demo.region.presentation.converter.RegionResultConverter;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RegionController.class)
@Import({
    RegionQueryConverter.class,
    RegionResultConverter.class,
    GlobalExceptionHandler.class,
    RegionControllerTest.MockBeans.class
})
class RegionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GetNearbyRegionUseCase getNearbyRegionUseCase;

    @BeforeEach
    void setUp() {
        when(getNearbyRegionUseCase.execute(any(NearbyRegionQuery.class)))
                .thenReturn(new NearbyRegionResult(
                        List.of(new NearbyRegionResult.Region(4413310500L, "천안시 서북구 성성동"))));
    }

    @Test
    void 좌표에_해당하는_법정동을_조회한다() throws Exception {
        mockMvc.perform(get("/api/v1/regions/nearby")
                        .queryParam("latitude", "36.8358")
                        .queryParam("longitude", "127.1324"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].regionId").isNumber())
                .andExpect(jsonPath("$[0].regionId").value(4413310500L))
                .andExpect(jsonPath("$[0].regionName").value("천안시 서북구 성성동"));
    }

    @Test
    void 좌표가_누락되면_v1_검증_오류를_응답한다() throws Exception {
        mockMvc.perform(get("/api/v1/regions/nearby").queryParam("latitude", "36.8358"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorType.INVALID_PARAMETER_ERROR.name()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void 좌표가_범위를_벗어나면_v1_검증_오류를_응답한다() throws Exception {
        mockMvc.perform(get("/api/v1/regions/nearby")
                        .queryParam("latitude", "91")
                        .queryParam("longitude", "127.1324"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorType.INVALID_PARAMETER_ERROR.name()));
    }

    @Test
    void 법정동이_없으면_빈_배열을_응답한다() throws Exception {
        when(getNearbyRegionUseCase.execute(any(NearbyRegionQuery.class)))
                .thenReturn(new NearbyRegionResult(List.of()));

        mockMvc.perform(get("/api/v1/regions/nearby")
                        .queryParam("latitude", "36.8358")
                        .queryParam("longitude", "127.1324"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class MockBeans {

        @Bean
        GetNearbyRegionUseCase getNearbyRegionUseCase() {
            return mock(GetNearbyRegionUseCase.class);
        }
    }
}
