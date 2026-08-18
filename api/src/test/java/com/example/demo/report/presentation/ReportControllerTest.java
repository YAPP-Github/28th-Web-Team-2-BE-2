package com.example.demo.report.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.auth.application.port.TokenProvider;
import com.example.demo.auth.application.result.AccessTokenPayload;
import com.example.demo.auth.domain.UserRole;
import com.example.demo.common.config.security.SecurityConfig;
import com.example.demo.common.exception.ErrorType;
import com.example.demo.common.security.JwtAccessDeniedHandler;
import com.example.demo.common.security.JwtAuthenticationEntryPoint;
import com.example.demo.common.security.SecurityErrorResponseWriter;
import com.example.demo.report.application.command.CreateUserReportCommand;
import com.example.demo.report.application.result.CreateUserReportResult;
import com.example.demo.report.application.usecase.CreateUserReportUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.demo.report.presentation.converter.UserReportCommandConverter;
import com.example.demo.report.presentation.converter.UserReportResultConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserReportController.class)
@Import({
    SecurityConfig.class,
    SecurityErrorResponseWriter.class,
    JwtAuthenticationEntryPoint.class,
    JwtAccessDeniedHandler.class,
    UserReportCommandConverter.class,
    UserReportResultConverter.class,
    ReportControllerTest.MockBeans.class
})
class ReportControllerTest {

    private static final String REPORT_PATH = "/api/v1/items/1/reports";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenProvider tokenProvider;

    @Autowired
    private CreateUserReportUseCase createUserReportUseCase;

    @BeforeEach
    void setUp() {
        clearInvocations(createUserReportUseCase);
        when(tokenProvider.parseAccessTokenPayload("access-token"))
                .thenReturn(new AccessTokenPayload(1L, UserRole.USER));
        when(createUserReportUseCase.execute(any()))
                .thenReturn(new CreateUserReportResult(42L));
    }

    @Test
    void 인증한_사용자가_품목_가격을_제보하면_created와_reportId를_응답한다() throws Exception {
        mockMvc.perform(post(REPORT_PATH)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"price":3500,"unit":"1kg","amount":2,"store":{"id":"16618597","placeName":"장보고 마트","addressName":"서울"},
                                "photoUrl":"https://images.example.com/reports/receipt.jpg"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("요청이 성공적으로 처리되었습니다."))
                .andExpect(jsonPath("$.data.reportId").isNumber());
    }

    @Test
    void 요청의_장소_스냅샷을_애플리케이션_명령으로_변환한다() throws Exception {
        mockMvc.perform(post(REPORT_PATH)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "price": 3500,
                                  "unit": "kg",
                                  "amount": 1.25,
                                  "store": {
                                    "id": "16618597",
                                    "placeName": "장생당약국",
                                    "placeUrl": "http://place.map.kakao.com/16618597",
                                    "categoryName": "의료,건강 > 약국",
                                    "addressName": "서울 강남구 대치동 943-16",
                                    "roadAddressName": "서울 강남구 테헤란로84길 17",
                                    "phone": "02-558-5476",
                                    "categoryGroupCode": "PM9",
                                    "categoryGroupName": "약국",
                                    "x": 127.0589707834,
                                    "y": 37.5060518881,
                                    "distance": 10
                                  },
                                  "photoUrl": "https://images.example.com/reports/receipt.jpg"
                                }
                                """))
                .andExpect(status().isCreated());

        final ArgumentCaptor<CreateUserReportCommand> captor = ArgumentCaptor.forClass(CreateUserReportCommand.class);
        verify(createUserReportUseCase).execute(captor.capture());
        final CreateUserReportCommand command = captor.getValue();
        assertThat(command.itemId()).isEqualTo(1L);
        assertThat(command.userId()).isEqualTo(1L);
        assertThat(command.amount()).isEqualByComparingTo("1.25");
        assertThat(command.store().kakaoPlaceId()).isEqualTo("16618597");
        assertThat(command.store().placeName()).isEqualTo("장생당약국");
        assertThat(command.store().roadAddressName()).isEqualTo("서울 강남구 테헤란로84길 17");
        assertThat(command.store().longitude()).isEqualByComparingTo("127.0589707834");
        assertThat(command.store().latitude()).isEqualByComparingTo("37.5060518881");
    }

    @Test
    void price가_누락되면_bad_request와_v1_검증_오류를_응답한다() throws Exception {
        mockMvc.perform(post(REPORT_PATH)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"unit":"1kg","amount":2,"store":{"id":"16618597","placeName":"장보고 마트","addressName":"서울"},
                                "photoUrl":"https://images.example.com/reports/receipt.jpg"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(ErrorType.INVALID_PARAMETER_ERROR.name()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void DB_길이를_초과한_Kakao_장소_ID는_bad_request로_거부한다() throws Exception {
        mockMvc.perform(post(REPORT_PATH)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"price":3500,"unit":"kg","amount":1,
                                 "store":{"id":"1234567890123456789012345678901","placeName":"장보고 마트","addressName":"서울"}}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorType.INVALID_PARAMETER_ERROR.name()));
    }

    @Test
    void 가격_amount_unit의_입력_제약을_위반하면_bad_request를_응답한다() throws Exception {
        mockMvc.perform(post(REPORT_PATH)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"price":-1,"unit":"   ","amount":0,
                                 "store":{"id":"16618597","placeName":"장보고 마트","addressName":"서울"}}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorType.INVALID_PARAMETER_ERROR.name()));
    }

    @Test
    void Kakao_소수점_14자리_좌표를_포함한_가격_제보를_허용한다() throws Exception {
        when(createUserReportUseCase.execute(any()))
                .thenReturn(new CreateUserReportResult(42L));

        mockMvc.perform(post(REPORT_PATH)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"price":10000,"unit":"1kg","amount":2,
                                 "store":{"id":"11840060","placeName":"롯데슈퍼프레시 코엑스점",
                                 "placeUrl":"http://place.map.kakao.com/11840060",
                                 "categoryName":"가정,생활 > 슈퍼마켓 > 대형슈퍼 > 롯데슈퍼프레시",
                                 "addressName":"서울 강남구 삼성동 107-6",
                                 "roadAddressName":"서울 강남구 봉은사로103길 5",
                                 "phone":"02-3446-5602","categoryGroupCode":"MT1",
                                 "categoryGroupName":"대형마트","x":"127.06140867761812",
                                 "y":"37.51504772738281","distance":711},
                                 "photoUrl":"http://place.map.kakao.com/11840060"}
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void 인증_없이_품목_가격을_제보하면_unauthorized와_v1_오류_응답을_응답한다() throws Exception {
        mockMvc.perform(post(REPORT_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"price":3500,"unit":"1kg","amount":2,"store":{"id":"16618597","placeName":"장보고 마트","addressName":"서울"},
                                "photoUrl":"https://images.example.com/reports/receipt.jpg"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(ErrorType.UNAUTHORIZED.name()))
                .andExpect(jsonPath("$.data").value((Object) null));
    }

    @TestConfiguration(proxyBeanMethods = false)
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
        CreateUserReportUseCase createUserReportUseCase() {
            return mock(CreateUserReportUseCase.class);
        }
    }
}
