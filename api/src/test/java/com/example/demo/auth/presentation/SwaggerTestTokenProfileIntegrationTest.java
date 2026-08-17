package com.example.demo.auth.presentation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "jwt.test-token-endpoint.enabled=true")
@AutoConfigureMockMvc
class SwaggerTestTokenProfileIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void 허용되지_않은_프로필에서는_설정이_켜져도_endpoint를_등록하지_않는다() throws Exception {
        mockMvc.perform(get("/api/auth/test/token").param("userId", "1"))
                .andExpect(status().isNotFound());
    }
}
