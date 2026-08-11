package com.example.demo.auth.presentation;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.auth.application.usecase.KakaoTestRedirectUseCase;
import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import com.example.demo.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class KakaoTestRedirectControllerTest {

    private final KakaoTestRedirectUseCase useCase = mock(KakaoTestRedirectUseCase.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new KakaoTestRedirectController(useCase))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void authorization_code를_idToken으로_교환하고_idToken만_응답한다() throws Exception {
        when(useCase.execute("authorization-code")).thenReturn("id-token");

        mockMvc.perform(get("/api/auth/test/kakao/redirect").param("code", "authorization-code"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"idToken\":\"id-token\"}"))
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andExpect(jsonPath("$.refreshToken").doesNotExist());
    }

    @Test
    void code가_없으면_잘못된_매개변수_오류를_응답한다() throws Exception {
        when(useCase.execute(null)).thenThrow(new ApiException(
                ErrorType.INVALID_PARAMETER_ERROR.description(),
                ErrorType.INVALID_PARAMETER_ERROR,
                HttpStatus.BAD_REQUEST));

        mockMvc.perform(get("/api/auth/test/kakao/redirect"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorType").value(ErrorType.INVALID_PARAMETER_ERROR.name()))
                .andExpect(jsonPath("$.errorMessage").value(containsString("잘못된")));
    }

    @Test
    void Kakao_교환_실패는_기존_Kakao_토큰_오류를_응답한다() throws Exception {
        when(useCase.execute("invalid-code")).thenThrow(new ApiException(
                ErrorType.KAKAO_TOKEN_INVALID.description(),
                ErrorType.KAKAO_TOKEN_INVALID,
                HttpStatus.UNAUTHORIZED));

        mockMvc.perform(get("/api/auth/test/kakao/redirect").param("code", "invalid-code"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorType").value(ErrorType.KAKAO_TOKEN_INVALID.name()))
                .andExpect(jsonPath("$.idToken").doesNotExist());
    }
}
