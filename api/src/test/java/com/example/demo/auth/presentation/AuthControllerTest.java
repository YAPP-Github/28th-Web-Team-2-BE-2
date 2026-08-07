package com.example.demo.auth.presentation;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.auth.application.command.KakaoLoginCommand;
import com.example.demo.auth.application.command.LogoutCommand;
import com.example.demo.auth.application.command.RefreshTokenCommand;
import com.example.demo.auth.application.result.AuthToken;
import com.example.demo.auth.application.usecase.KakaoLoginUseCase;
import com.example.demo.auth.application.usecase.LogoutUseCase;
import com.example.demo.auth.application.usecase.ReissueTokenUseCase;
import com.example.demo.auth.presentation.converter.AuthCommandConverter;
import com.example.demo.auth.presentation.converter.AuthResultConverter;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AuthControllerTest {

    private final KakaoLoginUseCase kakaoLoginUseCase = mock(KakaoLoginUseCase.class);
    private final ReissueTokenUseCase reissueTokenUseCase = mock(ReissueTokenUseCase.class);
    private final LogoutUseCase logoutUseCase = mock(LogoutUseCase.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        final RefreshTokenCookie cookie = new RefreshTokenCookie(true, Duration.ofDays(14));
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(
                        kakaoLoginUseCase,
                        reissueTokenUseCase,
                        logoutUseCase,
                        new AuthCommandConverter(),
                        new AuthResultConverter(),
                        cookie))
                .build();
    }

    @Test
    void Kakao_로그인은_Access_Token만_JSON으로_내리고_Refresh_Token을_보안_cookie로_설정한다()
            throws Exception {
        when(kakaoLoginUseCase.execute(new KakaoLoginCommand("id-token")))
                .thenReturn(new AuthToken("access-token", "refresh-token"));

        mockMvc.perform(post("/api/auth/kakao/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"id-token\"}"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"accessToken\":\"access-token\"}"))
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("refreshToken=refresh-token")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Secure")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("SameSite=Lax")));
    }

    @Test
    void 재발급은_cookie의_Refresh_Token을_소비하고_새_cookie를_설정한다() throws Exception {
        when(reissueTokenUseCase.execute(new RefreshTokenCommand("old-refresh-token")))
                .thenReturn(new AuthToken("new-access-token", "new-refresh-token"));

        mockMvc.perform(post("/api/auth/reissue").cookie(
                        new jakarta.servlet.http.Cookie(RefreshTokenCookie.NAME, "old-refresh-token")))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"accessToken\":\"new-access-token\"}"))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("refreshToken=new-refresh-token")));
    }

    @Test
    void 로그아웃은_Refresh_Token을_폐기하고_cookie를_삭제한다() throws Exception {
        mockMvc.perform(post("/api/auth/logout").cookie(
                        new jakarta.servlet.http.Cookie(RefreshTokenCookie.NAME, "refresh-token")))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")));

        verify(logoutUseCase).execute(new LogoutCommand("refresh-token"));
    }

    @Test
    void 빈_idToken은_검증_전에_400을_응답한다() throws Exception {
        mockMvc.perform(post("/api/auth/kakao/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\" \"}"))
                .andExpect(status().isBadRequest());
    }
}
