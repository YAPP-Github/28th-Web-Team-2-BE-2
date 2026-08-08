package com.example.demo.auth.presentation;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.auth.application.command.LoginCommand;
import com.example.demo.auth.application.command.RefreshTokenCommand;
import com.example.demo.auth.application.result.AuthToken;
import com.example.demo.auth.application.usecase.LoginUseCase;
import com.example.demo.auth.application.usecase.LogoutUseCase;
import com.example.demo.auth.application.usecase.ReissueTokenUseCase;
import com.example.demo.auth.domain.ProviderType;
import com.example.demo.auth.presentation.converter.AuthCommandConverter;
import com.example.demo.auth.presentation.converter.AuthResultConverter;
import com.example.demo.common.exception.GlobalExceptionHandler;
import com.example.demo.common.security.AuthPrincipal;
import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AuthControllerTest {

    private final LoginUseCase loginUseCase = mock(LoginUseCase.class);
    private final ReissueTokenUseCase reissueTokenUseCase = mock(ReissueTokenUseCase.class);
    private final LogoutUseCase logoutUseCase = mock(LogoutUseCase.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(
                        loginUseCase,
                        reissueTokenUseCase,
                        logoutUseCase,
                        new AuthCommandConverter(),
                        new AuthResultConverter(),
                        new RefreshTokenCookie(true, Duration.ofDays(14))))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void Kakao_로그인은_Access_Token을_JSON으로_내리고_Refresh_Token을_보안_cookie로_내린다()
            throws Exception {
        when(loginUseCase.execute(new LoginCommand(ProviderType.KAKAO, "id-token")))
                .thenReturn(new AuthToken("access-token", "refresh-token"));

        mockMvc.perform(post("/api/auth/kakao/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"id-token\"}"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"accessToken\":\"access-token\"}"))
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, allOf(
                        containsString("refreshToken=refresh-token"),
                        containsString("Path=/api/auth"),
                        containsString("Max-Age=1209600"),
                        containsString("Secure"),
                        containsString("HttpOnly"),
                        containsString("SameSite=Lax"))));
    }

    @Test
    void 재발급은_cookie_Refresh_Token을_받아_Access_Token과_새_cookie를_내린다() throws Exception {
        when(reissueTokenUseCase.execute(new RefreshTokenCommand("old-refresh-token")))
                .thenReturn(new AuthToken("new-access-token", "new-refresh-token"));

        mockMvc.perform(post("/api/auth/reissue")
                        .cookie(new Cookie("refreshToken", "old-refresh-token")))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"accessToken\":\"new-access-token\"}"))
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, allOf(
                        containsString("refreshToken=new-refresh-token"),
                        containsString("Path=/api/auth"),
                        containsString("Max-Age=1209600"),
                        containsString("Secure"),
                        containsString("HttpOnly"),
                        containsString("SameSite=Lax"))));
    }

    @Test
    void 로그아웃은_Redis_세션을_삭제하고_refresh_cookie를_만료한다() throws Exception {
        final var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(new AuthPrincipal(1L), null));
        SecurityContextHolder.setContext(context);

        try {
            mockMvc.perform(post("/api/auth/logout"))
                    .andExpect(status().isNoContent())
                    .andExpect(header().string(HttpHeaders.SET_COOKIE, allOf(
                            containsString("refreshToken="),
                            containsString("Path=/api/auth"),
                            containsString("Max-Age=0"),
                            containsString("Secure"),
                            containsString("HttpOnly"),
                            containsString("SameSite=Lax"))));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void 빈_idToken은_검증_전에_400을_응답한다() throws Exception {
        mockMvc.perform(post("/api/auth/kakao/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .content("{\"idToken\":\" \"}"))
                .andExpect(status().isBadRequest());
    }
}
