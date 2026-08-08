package com.example.demo.auth.presentation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.auth.infrastructure.token.JwtTokenProvider;
import com.example.demo.auth.domain.UserRole;
import org.springframework.http.HttpHeaders;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void 잘못된_Kakao_idToken으로_로그인하면_401을_응답한다() throws Exception {
        mockMvc.perform(post("/api/auth/kakao/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"invalid\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 지원하지_않는_provider는_400을_응답한다() throws Exception {
        mockMvc.perform(post("/api/auth/apple/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"id-token\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorType").value("INVALID_PARAMETER_ERROR"));
    }

    @Test
    void 기존_API는_인증이_없어도_공개로_응답한다() throws Exception {
        mockMvc.perform(get("/api/samples"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void 유효한_Access_Token으로_보호된_API를_호출할_수_있다() throws Exception {
        final String accessToken = jwtTokenProvider.createAccessToken(1L, UserRole.USER);

        mockMvc.perform(get("/api/samples").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void 잘못된_Access_Token은_공개_API를_막지_않는다() throws Exception {
        mockMvc.perform(get("/api/samples").header(HttpHeaders.AUTHORIZATION, "Bearer invalid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void 로그아웃은_Access_Token이_없으면_401을_응답한다() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorType").value("UNAUTHORIZED"));
    }
}
