package com.example.demo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class DemoApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoads() {
    }

    @Test
    void actuator_health를_제공한다() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void actuator_root는_인증없이_접근할수없다() throws Exception {
        mockMvc.perform(get("/actuator"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void swagger_ui와_openapi_문서를_설정된_메타데이터와_함께_제공한다() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));

        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.info.title").value("Demo API"))
                .andExpect(jsonPath("$.info.version").value("1.0.0"))
                .andExpect(jsonPath("$.paths['/api/samples']").exists())
                .andExpect(jsonPath("$.paths['/api/samples'].post.tags[0]").value("Sample"))
                .andExpect(jsonPath("$.paths['/api/samples'].post.summary")
                        .value("샘플 메시지를 생성한다"))
                .andExpect(jsonPath("$.paths['/api/samples'].post.responses['201'].description")
                        .value("샘플 메시지 생성 성공"))
                .andExpect(jsonPath("$.paths['/api/samples'].post.responses['400'].description")
                        .value("message가 비어 있으면 요청이 거부된다"))
                .andExpect(jsonPath("$.paths['/api/samples'].get.tags[0]").value("Sample"))
                .andExpect(jsonPath("$.paths['/api/samples'].get.summary")
                        .value("샘플 메시지를 조회한다"))
                .andExpect(jsonPath("$.paths['/api/samples'].get.responses['200'].description")
                        .value("샘플 메시지 조회 성공"))
                .andExpect(jsonPath("$.paths['/api/kamis/daily-prices'].get").exists())
                .andExpect(jsonPath("$.paths['/api/kamis/daily-prices'].get.summary")
                        .value("KAMIS 일별 부류별 가격을 조회한다"))
                .andExpect(jsonPath("$.paths['/api/kamis/daily-prices'].get.responses['200'].description")
                        .value("가격 조회 성공"))
                .andExpect(jsonPath("$.paths['/api/v1/regions/nearby'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/regions/nearby'].get.summary")
                        .value("좌표에 해당하는 법정동을 조회한다"))
                .andExpect(jsonPath("$.paths['/api/auth/{providerType}/login'].post.tags[0]").value("Auth"))
                .andExpect(jsonPath("$.paths['/api/auth/{providerType}/login'].post.summary")
                        .value("OAuth provider의 idToken으로 로그인한다"))
                .andExpect(jsonPath("$.paths['/api/auth/reissue'].post.summary")
                        .value("Refresh Token으로 Access Token을 재발급한다"))
                .andExpect(jsonPath("$.paths['/api/auth/reissue'].post.parameters[0].name")
                        .value("refreshToken"))
                .andExpect(jsonPath("$.paths['/api/auth/reissue'].post.parameters[0].in")
                        .value("cookie"))
                .andExpect(jsonPath("$.paths['/api/auth/logout'].post.summary")
                        .value("현재 사용자의 Refresh Token을 폐기하고 로그아웃한다"))
                .andExpect(jsonPath("$.paths['/api/auth/test/kakao/redirect']")
                        .doesNotExist());

        mockMvc.perform(get("/v3/api-docs/general"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/samples']").exists())
                .andExpect(jsonPath("$.paths['/api/auth/test/kakao/redirect']")
                        .doesNotExist());
    }

}
