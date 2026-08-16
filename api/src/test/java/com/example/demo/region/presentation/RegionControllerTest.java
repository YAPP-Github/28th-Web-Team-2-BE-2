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
import com.example.demo.common.exception.ApiException;
import com.example.demo.region.application.query.NearbyRegionQuery;
import com.example.demo.region.application.query.RegionSearchQuery;
import com.example.demo.region.application.result.NearbyRegionResult;
import com.example.demo.region.application.result.RegionSearchResult;
import com.example.demo.region.application.usecase.GetNearbyRegionUseCase;
import com.example.demo.region.application.usecase.SearchRegionsUseCase;
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
import org.springframework.http.HttpStatus;
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

    @Autowired
    private SearchRegionsUseCase searchRegionsUseCase;

    @BeforeEach
    void setUp() {
        when(getNearbyRegionUseCase.execute(any(NearbyRegionQuery.class)))
                .thenReturn(new NearbyRegionResult(
                        List.of(new NearbyRegionResult.Region(4413310500L, "천안시 서북구 성성동"))));
        when(searchRegionsUseCase.execute(any(RegionSearchQuery.class)))
                .thenReturn(new RegionSearchResult(List.of(
                        new RegionSearchResult.Region("4413310500", "천안시 서북구 성성동"),
                        new RegionSearchResult.Region("4413310600", "천안시 동남구 성성동"))));
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

    @Test
    void 비로그인_사용자가_키워드로_법정동을_검색하면_wrapper의_검색_결과를_응답한다() throws Exception {
        mockMvc.perform(get("/api/v1/regions/search").queryParam("keyword", "성성동"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.data.searchResults").isArray())
                .andExpect(jsonPath("$.data.searchResults[0].regionId").value("4413310500"))
                .andExpect(jsonPath("$.data.searchResults[0].regionName").value("천안시 서북구 성성동"));
    }

    @Test
    void 키워드의_일부만_입력해도_일치하는_법정동_검색_결과를_응답한다() throws Exception {
        mockMvc.perform(get("/api/v1/regions/search").queryParam("keyword", "성성"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.data.searchResults").isArray())
                .andExpect(jsonPath("$.data.searchResults.length()").value(2))
                .andExpect(jsonPath("$.data.searchResults[0].regionName").value("천안시 서북구 성성동"))
                .andExpect(jsonPath("$.data.searchResults[1].regionName").value("천안시 동남구 성성동"));
    }

    @Test
    void 일치하는_법정동이_없으면_빈_검색_결과를_응답한다() throws Exception {
        when(searchRegionsUseCase.execute(any(RegionSearchQuery.class)))
                .thenReturn(new RegionSearchResult(List.of()));
        mockMvc.perform(get("/api/v1/regions/search").queryParam("keyword", "없는법정동"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.data.searchResults").isArray())
                .andExpect(jsonPath("$.data.searchResults").isEmpty());
    }

    @Test
    void 키워드가_누락되면_v1_검증_오류와_본문_없는_data를_응답한다() throws Exception {
        mockMvc.perform(get("/api/v1/regions/search"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(ErrorType.INVALID_PARAMETER_ERROR.name()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void 키워드가_공백이면_v1_검증_오류와_본문_없는_data를_응답한다() throws Exception {
        mockMvc.perform(get("/api/v1/regions/search").queryParam("keyword", " "))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(ErrorType.INVALID_PARAMETER_ERROR.name()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void 키워드에_허용되지_않은_문자가_포함되면_v1_검증_오류를_응답한다() throws Exception {
        mockMvc.perform(get("/api/v1/regions/search").queryParam("keyword", "성성동!"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(ErrorType.INVALID_PARAMETER_ERROR.name()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void 키워드가_최대_길이를_초과하면_v1_검증_오류를_응답한다() throws Exception {
        mockMvc.perform(get("/api/v1/regions/search").queryParam("keyword", "가".repeat(31)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(ErrorType.INVALID_PARAMETER_ERROR.name()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void provider_오류는_v1_외부_API_오류를_응답한다() throws Exception {
        when(searchRegionsUseCase.execute(any(RegionSearchQuery.class)))
                .thenThrow(new ApiException(
                        ErrorType.EXTERNAL_API_ERROR.description(),
                        ErrorType.EXTERNAL_API_ERROR,
                        HttpStatus.BAD_GATEWAY));

        mockMvc.perform(get("/api/v1/regions/search").queryParam("keyword", "성성동"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value(ErrorType.EXTERNAL_API_ERROR.name()))
                .andExpect(jsonPath("$.message").value(ErrorType.EXTERNAL_API_ERROR.description()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class MockBeans {

        @Bean
        GetNearbyRegionUseCase getNearbyRegionUseCase() {
            return mock(GetNearbyRegionUseCase.class);
        }

        @Bean
        SearchRegionsUseCase searchRegionsUseCase() {
            return mock(SearchRegionsUseCase.class);
        }
    }
}
