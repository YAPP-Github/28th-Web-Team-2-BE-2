package com.example.demo.auth.presentation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.auth.infrastructure.token.JwtTokenProvider;
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
    void 보호된_API는_인증이_없으면_JSON_401을_응답한다() throws Exception {
        mockMvc.perform(get("/api/samples"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorType").value("UNAUTHORIZED"));
    }

    @Test
    void 유효한_Access_Token으로_보호된_API를_호출할_수_있다() throws Exception {
        final String accessToken = jwtTokenProvider.createAccessToken("kakao-subject");

        mockMvc.perform(get("/api/samples").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void 잘못된_Access_Token은_인증_실패_JSON을_응답한다() throws Exception {
        mockMvc.perform(get("/api/samples").header(HttpHeaders.AUTHORIZATION, "Bearer invalid"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorType").value("INVALID_TOKEN"));
    }
}
