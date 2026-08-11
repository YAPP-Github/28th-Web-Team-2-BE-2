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
    void refresh_cookie가_없으면_재발급은_401을_응답한다() throws Exception {
        mockMvc.perform(post("/api/auth/reissue"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 인증_토큰이_없으면_기본_보호_API는_401을_응답한다() throws Exception {
        mockMvc.perform(get("/api/samples"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorType").value("UNAUTHORIZED"));
    }

    @Test
    void 유효한_Access_Token으로_보호된_API를_호출할_수_있다() throws Exception {
        final String accessToken = jwtTokenProvider.createAccessToken(1L, UserRole.USER);

        mockMvc.perform(get("/api/samples").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void 잘못된_Access_Token은_보호_API에서_401을_응답한다() throws Exception {
        mockMvc.perform(get("/api/samples").header(HttpHeaders.AUTHORIZATION, "Bearer invalid"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorType").value("INVALID_TOKEN"));
    }

    @Test
    void Kakao_test_redirect가_비활성화되면_endpoint를_등록하지_않는다() throws Exception {
        mockMvc.perform(get("/api/auth/test/kakao/redirect").param("code", "authorization-code"))
                .andExpect(status().isNotFound());
    }

    @Test
    void 로그아웃은_Access_Token이_없으면_401을_응답한다() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorType").value("UNAUTHORIZED"));
    }
}
