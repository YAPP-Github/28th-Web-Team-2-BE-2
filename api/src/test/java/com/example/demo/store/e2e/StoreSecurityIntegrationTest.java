package com.example.demo.store.e2e;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class StoreSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void onlyLiked는_인증없이_호출하면_공통_401_응답을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/stores/nearby")
                        .queryParam("latitude", "37.5")
                        .queryParam("longitude", "127.0")
                        .queryParam("onlyLiked", "true"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void 공개_경로의_잘못된_좌표는_공통_400으로_처리된다() throws Exception {
        mockMvc.perform(get("/api/v1/stores/nearby")
                        .queryParam("latitude", "91")
                        .queryParam("longitude", "127"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER_ERROR"));
    }

    @Test
    void 잘못된_토큰은_공개_조회에서도_401을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/stores/nearby")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token")
                        .queryParam("latitude", "37.5")
                        .queryParam("longitude", "127.0"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"))
                .andExpect(jsonPath("$.data").value(nullValue()));

        mockMvc.perform(get("/api/v1/stores/nearby")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token")
                        .queryParam("latitude", "37.5")
                        .queryParam("longitude", "127.0")
                        .queryParam("onlyLiked", "true"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void OpenAPI에_로컬_동기화_500_오류를_노출한다() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                                "$.paths['/api/v1/stores/nearby'].get.parameters[?(@.name == 'keyword')]")
                        .exists())
                .andExpect(jsonPath("$.paths['/api/v1/stores/nearby'].get.responses['500']").exists());
    }
}
