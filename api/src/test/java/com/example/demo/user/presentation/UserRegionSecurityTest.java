package com.example.demo.user.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.auth.application.port.TokenProvider;
import com.example.demo.auth.application.result.AccessTokenPayload;
import com.example.demo.auth.domain.UserRole;
import com.example.demo.common.config.security.SecurityConfig;
import com.example.demo.common.security.JwtAccessDeniedHandler;
import com.example.demo.common.security.JwtAuthenticationEntryPoint;
import com.example.demo.common.security.SecurityErrorResponseWriter;
import com.example.demo.user.application.query.GetUserRegionsQuery;
import com.example.demo.user.application.result.GetUserRegionsResult;
import com.example.demo.user.application.usecase.AddUserRegionUseCase;
import com.example.demo.user.application.usecase.GetUserRegionsUseCase;
import com.example.demo.user.application.usecase.SetCurrentUserRegionUseCase;
import com.example.demo.user.presentation.command.UserRegionCommandConverter;
import com.example.demo.user.presentation.converter.UserRegionQueryConverter;
import com.example.demo.user.presentation.converter.UserRegionResultConverter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserRegionController.class)
@Import({
    SecurityConfig.class,
    SecurityErrorResponseWriter.class,
    JwtAuthenticationEntryPoint.class,
    JwtAccessDeniedHandler.class,
    UserRegionSecurityTest.MockBeans.class
})
class UserRegionSecurityTest {

    private static final String PATH = "/api/v1/users/me/regions";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenProvider tokenProvider;

    @Autowired
    private GetUserRegionsUseCase getUserRegionsUseCase;

    @BeforeEach
    void setUp() {
        when(tokenProvider.parseAccessTokenPayload("access-token"))
                .thenReturn(new AccessTokenPayload(1L, UserRole.USER));
        when(getUserRegionsUseCase.execute(any(GetUserRegionsQuery.class)))
                .thenReturn(new GetUserRegionsResult(List.of()));
    }

    @Test
    void HEAD_관심_지역_조회는_비로그인과_잘못된_JWT에_401을_응답한다() throws Exception {
        mockMvc.perform(head(PATH))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(head(PATH).header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void HEAD_관심_지역_조회는_ROLE_GUEST에_403을_응답한다() throws Exception {
        mockMvc.perform(head(PATH).with(SecurityMockMvcRequestPostProcessors.user("guest").roles("GUEST")))
                .andExpect(status().isForbidden());
    }

    @Test
    void HEAD_관심_지역_조회는_ROLE_USER만_통과시킨다() throws Exception {
        mockMvc.perform(head(PATH).header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
                .andExpect(status().isOk());
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class MockBeans {

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        TokenProvider tokenProvider() {
            return mock(TokenProvider.class);
        }

        @Bean
        AddUserRegionUseCase addUserRegionUseCase() {
            return mock(AddUserRegionUseCase.class);
        }

        @Bean
        GetUserRegionsUseCase getUserRegionsUseCase() {
            return mock(GetUserRegionsUseCase.class);
        }

        @Bean
        SetCurrentUserRegionUseCase setCurrentUserRegionUseCase() {
            return mock(SetCurrentUserRegionUseCase.class);
        }

        @Bean
        UserRegionCommandConverter userRegionCommandConverter() {
            return mock(UserRegionCommandConverter.class);
        }

        @Bean
        UserRegionQueryConverter userRegionQueryConverter() {
            return mock(UserRegionQueryConverter.class);
        }

        @Bean
        UserRegionResultConverter userRegionResultConverter() {
            return mock(UserRegionResultConverter.class);
        }
    }
}
