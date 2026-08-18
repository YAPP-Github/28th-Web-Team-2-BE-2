package com.example.demo.auth.presentation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "jwt.test-token-endpoint.enabled=true")
@AutoConfigureMockMvc
@ActiveProfiles("swagger-test")
class SwaggerTestTokenIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void 설정이_켜지면_Swagger_테스트용_access_token을_발급한다() throws Exception {
        mockMvc.perform(get("/api/auth/test/token").param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString());
    }
}
