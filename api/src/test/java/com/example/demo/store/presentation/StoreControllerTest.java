package com.example.demo.store.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.common.exception.ErrorType;
import com.example.demo.common.exception.GlobalExceptionHandler;
import com.example.demo.store.application.query.NearbyStoreQuery;
import com.example.demo.store.application.result.NearbyStoreResult;
import com.example.demo.store.application.result.NearbyStoresResult;
import com.example.demo.store.application.usecase.GetNearbyStoresUseCase;
import com.example.demo.store.presentation.converter.StoreQueryConverter;
import com.example.demo.store.presentation.converter.StoreResultConverter;
import java.math.BigDecimal;
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

@WebMvcTest(StoreController.class)
@Import({
    StoreQueryConverter.class,
    StoreResultConverter.class,
    GlobalExceptionHandler.class,
    StoreControllerTest.MockBeans.class
})
class StoreControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GetNearbyStoresUseCase getNearbyStoresUseCase;

    @BeforeEach
    void setUp() {
        when(getNearbyStoresUseCase.execute(any(NearbyStoreQuery.class)))
                .thenReturn(new NearbyStoresResult(
                        1,
                        List.of(new NearbyStoreResult(
                                "store-1",
                                "장보고 마트",
                                new BigDecimal("37.5088"),
                                new BigDecimal("127.0632"),
                                "서울 강남구 삼성동 123",
                                "서울 강남구 테헤란로 123",
                                "02-1234-5678",
                                "http://place.map.kakao.com/store-1",
                                670,
                                false))));
    }

    @Test
    void 비로그인_사용자가_주변_가게를_조회하면_ok와_가게_목록을_응답한다() throws Exception {
        mockMvc.perform(get("/api/v1/stores/nearby")
                        .queryParam("latitude", "37.5088")
                        .queryParam("longitude", "127.0632")
                        .queryParam("radius", "1500"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.stores[0].storeId").value("store-1"))
                .andExpect(jsonPath("$.stores[0].storeName").value("장보고 마트"))
                .andExpect(jsonPath("$.stores[0].latitude").value(37.5088))
                .andExpect(jsonPath("$.stores[0].longitude").value(127.0632))
                .andExpect(jsonPath("$.stores[0].addressName").value("서울 강남구 삼성동 123"))
                .andExpect(jsonPath("$.stores[0].roadAddressName").value("서울 강남구 테헤란로 123"))
                .andExpect(jsonPath("$.stores[0].phone").value("02-1234-5678"))
                .andExpect(jsonPath("$.stores[0].placeUrl").value("http://place.map.kakao.com/store-1"))
                .andExpect(jsonPath("$.stores[0].distanceMeters").value(670))
                .andExpect(jsonPath("$.stores[0].isLiked").value(false));
    }

    @Test
    void 위도가_누락되면_bad_request와_v1_검증_오류를_응답한다() throws Exception {
        mockMvc.perform(get("/api/v1/stores/nearby").queryParam("longitude", "127.0632"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(ErrorType.INVALID_PARAMETER_ERROR.name()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void 반경이_허용_범위를_초과하면_bad_request와_v1_검증_오류를_응답한다() throws Exception {
        mockMvc.perform(get("/api/v1/stores/nearby")
                        .queryParam("latitude", "37.5088")
                        .queryParam("longitude", "127.0632")
                        .queryParam("radius", "20001"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(ErrorType.INVALID_PARAMETER_ERROR.name()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class MockBeans {

        @Bean
        GetNearbyStoresUseCase getNearbyStoresUseCase() {
            return mock(GetNearbyStoresUseCase.class);
        }

    }
}
