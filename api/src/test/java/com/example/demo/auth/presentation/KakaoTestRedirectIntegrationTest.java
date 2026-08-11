package com.example.demo.auth.presentation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.common.exception.ErrorType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "kakao.oauth.test-endpoint.enabled=true")
@AutoConfigureMockMvc
class KakaoTestRedirectIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void 설정이_켜지면_test_redirect_endpoint가_등록되고_빈_code를_검증한다() throws Exception {
        mockMvc.perform(get("/api/auth/test/kakao/redirect").param("code", " "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorType").value(ErrorType.INVALID_PARAMETER_ERROR.name()));
    }
}
