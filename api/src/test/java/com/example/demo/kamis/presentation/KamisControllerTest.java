package com.example.demo.kamis.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import com.example.demo.common.exception.GlobalExceptionHandler;
import com.example.demo.kamis.application.query.KamisDailyPriceQuery;
import com.example.demo.kamis.application.result.KamisDailyPriceItemResult;
import com.example.demo.kamis.application.result.KamisDailyPriceResult;
import com.example.demo.kamis.application.usecase.GetKamisDailyPriceUseCase;
import com.example.demo.kamis.presentation.converter.KamisDailyPriceConverter;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(KamisController.class)
@Import({KamisDailyPriceConverter.class, GlobalExceptionHandler.class, KamisControllerTest.MockBeans.class})
class KamisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GetKamisDailyPriceUseCase getKamisDailyPriceUseCase;

    @BeforeEach
    void setUp() {
        when(getKamisDailyPriceUseCase.execute(any(KamisDailyPriceQuery.class)))
                .thenReturn(new KamisDailyPriceResult(
                        "000",
                        null,
                        List.of(new KamisDailyPriceItemResult(
                                "양파",
                                "211",
                                "양파",
                                "01",
                                "상품",
                                "1kg",
                                "2026-08-06",
                                "3,000",
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null))));
    }

    @Test
    void KAMIS_일별_가격을_조회한다() throws Exception {
        mockMvc.perform(get("/api/kamis/daily-prices")
                        .queryParam("productClsCode", "02")
                        .queryParam("itemCategoryCode", "200")
                        .queryParam("countryCode", "1101")
                        .queryParam("regDay", "2015-10-01")
                        .queryParam("convertKgYn", "N"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errorCode").value("000"))
                .andExpect(jsonPath("$.items[0].itemName").value("양파"))
                .andExpect(jsonPath("$.items[0].dpr1").value("3,000"));
    }

    @Test
    void 잘못된_가격_구분은_bad_request를_응답한다() throws Exception {
        mockMvc.perform(get("/api/kamis/daily-prices")
                        .queryParam("productClsCode", "03"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void KAMIS_외부_예외는_외부_API_오류_응답으로_변환한다() throws Exception {
        when(getKamisDailyPriceUseCase.execute(any(KamisDailyPriceQuery.class)))
                .thenThrow(new ApiException(
                        ErrorType.EXTERNAL_API_ERROR.description(),
                        ErrorType.EXTERNAL_API_ERROR,
                        HttpStatus.BAD_GATEWAY));

        mockMvc.perform(get("/api/kamis/daily-prices")
                        .queryParam("productClsCode", "02")
                        .queryParam("itemCategoryCode", "200")
                        .queryParam("countryCode", "1101")
                        .queryParam("regDay", "2015-10-01")
                        .queryParam("convertKgYn", "N"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.errorType").value("EXTERNAL_API_ERROR"))
                .andExpect(jsonPath("$.errorMessage").value(ErrorType.EXTERNAL_API_ERROR.description()));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class MockBeans {

        @Bean
        GetKamisDailyPriceUseCase getKamisDailyPriceUseCase() {
            return mock(GetKamisDailyPriceUseCase.class);
        }
    }
}
