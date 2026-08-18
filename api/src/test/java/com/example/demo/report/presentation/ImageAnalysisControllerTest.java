package com.example.demo.report.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.auth.application.port.TokenProvider;
import com.example.demo.auth.application.result.AccessTokenPayload;
import com.example.demo.auth.domain.UserRole;
import com.example.demo.common.config.security.SecurityConfig;
import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import com.example.demo.common.security.JwtAccessDeniedHandler;
import com.example.demo.common.security.JwtAuthenticationEntryPoint;
import com.example.demo.common.security.SecurityErrorResponseWriter;
import com.example.demo.report.application.contract.ItemCandidate;
import com.example.demo.report.application.result.ImageAnalysisResult;
import com.example.demo.report.application.usecase.AnalyzeReportImageUseCase;
import com.example.demo.report.domain.AnalysisConfidence;
import com.example.demo.report.presentation.converter.ImageAnalysisConverter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserReportImageAnalysisController.class)
@Import({
    SecurityConfig.class,
    SecurityErrorResponseWriter.class,
    JwtAuthenticationEntryPoint.class,
    JwtAccessDeniedHandler.class,
    ImageAnalysisConverter.class,
    ImageAnalysisControllerTest.MockBeans.class
})
class ImageAnalysisControllerTest {

    private static final String PATH = "/api/v1/user-reports/image-analysis";
    private static final String BODY = """
            {"imageUrl":"https://cdn.example.com/images/abc.jpg"}""";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenProvider tokenProvider;

    @Autowired
    private AnalyzeReportImageUseCase analyzeReportImageUseCase;

    @BeforeEach
    void setUp() {
        reset(analyzeReportImageUseCase);
        when(tokenProvider.parseAccessTokenPayload("access-token"))
                .thenReturn(new AccessTokenPayload(1L, UserRole.USER));
    }

    @Test
    void 인식에_성공하면_품목_가격_단위를_응답한다() throws Exception {
        when(analyzeReportImageUseCase.execute(any())).thenReturn(new ImageAnalysisResult(
                new ItemCandidate(12L, "오이", "1개"),
                new AnalysisConfidence(new BigDecimal("0.96")),
                List.of(new ItemCandidate(12L, "오이", "1개")),
                250,
                new AnalysisConfidence(new BigDecimal("0.95")),
                "1개",
                new BigDecimal("1"),
                new AnalysisConfidence(new BigDecimal("0.72"))));

        mockMvc.perform(authorized())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.item.itemId").value(12))
                .andExpect(jsonPath("$.data.item.name").value("오이"))
                .andExpect(jsonPath("$.data.item.unit").value("1개"))
                .andExpect(jsonPath("$.data.price.value").value(250))
                .andExpect(jsonPath("$.data.price.currency").value("KRW"))
                .andExpect(jsonPath("$.data.amount.value").value(1));
    }

    // envelope가 이미 code·message를 주므로 payload에 중복해 두지 않는다.
    @Test
    void payload에_status와_message를_두지_않는다() throws Exception {
        when(analyzeReportImageUseCase.execute(any())).thenReturn(new ImageAnalysisResult(
                null, null, List.of(), null, null, null, null, null));

        mockMvc.perform(authorized())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").doesNotExist())
                .andExpect(jsonPath("$.data.message").doesNotExist());
    }

    @Test
    void 부분_인식이면_인식하지_못한_값을_null로_응답한다() throws Exception {
        when(analyzeReportImageUseCase.execute(any())).thenReturn(new ImageAnalysisResult(
                new ItemCandidate(12L, "오이", "1개"),
                new AnalysisConfidence(new BigDecimal("0.96")),
                List.of(new ItemCandidate(12L, "오이", "1개")),
                250,
                new AnalysisConfidence(new BigDecimal("0.95")),
                "1개",
                null,
                null));

        mockMvc.perform(authorized())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.price.value").value(250))
                .andExpect(jsonPath("$.data.amount").doesNotExist());
    }

    @Test
    void 후보가_여럿이면_item은_비우고_candidates를_응답한다() throws Exception {
        when(analyzeReportImageUseCase.execute(any())).thenReturn(new ImageAnalysisResult(
                null,
                AnalysisConfidence.low(),
                List.of(new ItemCandidate(10L, "애호박", "1개"), new ItemCandidate(11L, "쥬키니", "1개")),
                3000,
                AnalysisConfidence.low(),
                null,
                null,
                null));

        mockMvc.perform(authorized())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.item").doesNotExist())
                .andExpect(jsonPath("$.data.candidates.length()").value(2))
                .andExpect(jsonPath("$.data.candidates[0].unit").value("1개"));
    }

    @Test
    void 로그인하지_않으면_인식을_거부한다() throws Exception {
        mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());

        verify(analyzeReportImageUseCase, never()).execute(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"imageUrl\":\"\"}", "{\"imageUrl\":\"https://a\",\"itemId\":0}"})
    void 필수값이_빠지거나_잘못되면_거부한다(final String body) throws Exception {
        mockMvc.perform(post(PATH)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorType.INVALID_PARAMETER_ERROR.name()));

        verify(analyzeReportImageUseCase, never()).execute(any());
    }

    @Test
    void timeout은_504와_전용_코드로_응답한다() throws Exception {
        givenFailure(ErrorType.IMAGE_ANALYSIS_TIMEOUT, HttpStatus.GATEWAY_TIMEOUT);

        mockMvc.perform(authorized())
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.code").value(ErrorType.IMAGE_ANALYSIS_TIMEOUT.name()));
    }

    @Test
    void rate_limit은_429와_전용_코드로_응답한다() throws Exception {
        givenFailure(ErrorType.IMAGE_ANALYSIS_RATE_LIMITED, HttpStatus.TOO_MANY_REQUESTS);

        mockMvc.perform(authorized())
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value(ErrorType.IMAGE_ANALYSIS_RATE_LIMITED.name()));
    }

    @Test
    void 모델_응답을_해석할_수_없으면_502로_응답한다() throws Exception {
        givenFailure(ErrorType.IMAGE_ANALYSIS_UNAVAILABLE, HttpStatus.BAD_GATEWAY);

        mockMvc.perform(authorized())
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value(ErrorType.IMAGE_ANALYSIS_UNAVAILABLE.name()));
    }

    private void givenFailure(final ErrorType errorType, final HttpStatus status) {
        when(analyzeReportImageUseCase.execute(any()))
                .thenThrow(new ApiException(errorType.description(), errorType, status));
    }

    private org.springframework.test.web.servlet.RequestBuilder authorized() {
        return post(PATH)
                .header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY);
    }

    @TestConfiguration
    static class MockBeans {

        @Bean
        TokenProvider tokenProvider() {
            return mock(TokenProvider.class);
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        AnalyzeReportImageUseCase analyzeReportImageUseCase() {
            return mock(AnalyzeReportImageUseCase.class);
        }
    }
}
