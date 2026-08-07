package com.example.demo.auth.presentation;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.auth.application.command.KakaoLoginCommand;
import com.example.demo.auth.application.command.RefreshTokenCommand;
import com.example.demo.auth.application.result.AuthToken;
import com.example.demo.auth.application.usecase.KakaoLoginUseCase;
import com.example.demo.auth.application.usecase.LogoutUseCase;
import com.example.demo.auth.application.usecase.ReissueTokenUseCase;
import com.example.demo.auth.presentation.converter.AuthCommandConverter;
import com.example.demo.auth.presentation.converter.AuthResultConverter;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AuthControllerTest {

    private final KakaoLoginUseCase kakaoLoginUseCase = mock(KakaoLoginUseCase.class);
    private final ReissueTokenUseCase reissueTokenUseCase = mock(ReissueTokenUseCase.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(
                        kakaoLoginUseCase,
                        reissueTokenUseCase,
                        mock(LogoutUseCase.class),
                        new AuthCommandConverter(),
                        new AuthResultConverter()))
                .build();
    }

    @Test
    void Kakao_로그인은_Access_Token과_Refresh_Token을_JSON으로_내린다()
            throws Exception {
        when(kakaoLoginUseCase.execute(new KakaoLoginCommand("id-token")))
                .thenReturn(new AuthToken("access-token", "refresh-token"));

        mockMvc.perform(post("/api/auth/kakao/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"id-token\"}"))
                .andExpect(status().isOk())
                .andExpect(content().json(
                        "{\"accessToken\":\"access-token\",\"refreshToken\":\"refresh-token\"}"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"));
    }

    @Test
    void 재발급은_JSON_Refresh_Token을_받아_두_토큰을_내린다() throws Exception {
        when(reissueTokenUseCase.execute(new RefreshTokenCommand("old-refresh-token")))
                .thenReturn(new AuthToken("new-access-token", "new-refresh-token"));

        mockMvc.perform(post("/api/auth/reissue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"old-refresh-token\"}"))
                .andExpect(status().isOk())
                .andExpect(content().json(
                        "{\"accessToken\":\"new-access-token\",\"refreshToken\":\"new-refresh-token\"}"));
    }

    @Test
    void 빈_idToken은_검증_전에_400을_응답한다() throws Exception {
        mockMvc.perform(post("/api/auth/kakao/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .content("{\"idToken\":\" \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 빈_refresh_token은_검증_전에_400을_응답한다() throws Exception {
        mockMvc.perform(post("/api/auth/reissue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\" \"}"))
                .andExpect(status().isBadRequest());
    }
}
