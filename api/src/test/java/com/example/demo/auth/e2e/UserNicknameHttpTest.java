package com.example.demo.auth.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.auth.domain.ProviderType;
import com.example.demo.auth.domain.User;
import com.example.demo.auth.domain.UserRole;
import com.example.demo.auth.infrastructure.persistence.UserJpaRepository;
import com.example.demo.auth.infrastructure.token.JwtTokenProvider;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
class UserNicknameHttpTest {

    private static final String PATH = "/api/v1/users/me";

    private final MockMvc mockMvc;
    private final UserJpaRepository userJpaRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Autowired
    UserNicknameHttpTest(
            final MockMvc mockMvc,
            final UserJpaRepository userJpaRepository,
            final JwtTokenProvider jwtTokenProvider) {
        this.mockMvc = mockMvc;
        this.userJpaRepository = userJpaRepository;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Test
    void 인증_사용자는_닉네임을_저장하고_204를_응답한다() throws Exception {
        final User user = userJpaRepository.save(User.oauth(
                ProviderType.KAKAO,
                UUID.randomUUID().toString(),
                UUID.randomUUID() + "@example.com",
                "닉네임 사용자"));
        final String token = jwtTokenProvider.createAccessToken(user.id(), user.role());

        mockMvc.perform(patch(PATH)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"장보고01\"}"))
                .andExpect(status().isNoContent());

        assertThat(userJpaRepository.findById(user.id()).orElseThrow().nickname()).isEqualTo("장보고01");
        mockMvc.perform(patch(PATH)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"Market01\"}"))
                .andExpect(status().isNoContent());
        mockMvc.perform(patch(PATH)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"  Market01  \"}"))
                .andExpect(status().isNoContent());
        assertThat(userJpaRepository.findById(user.id()).orElseThrow().nickname()).isEqualTo("Market01");
    }

    @Test
    void 닉네임_앞뒤_공백은_제거하고_형식이_잘못되면_400을_응답한다() throws Exception {
        final String token = accessToken(saveUser("닉네임 형식 사용자"));

        updateNickname(token, "  형식통과01  ")
                .andExpect(status().isNoContent());
        updateNickname(token, "  ")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER_ERROR"));
        updateNickname(token, "a").andExpect(status().isBadRequest());
        updateNickname(token, "nickname-too-long").andExpect(status().isBadRequest());
        updateNickname(token, "닉네임!").andExpect(status().isBadRequest());
    }

    @Test
    void 중복_닉네임은_409이고_기존_닉네임은_보존된다() throws Exception {
        final User firstUser = saveUser("첫 닉네임 사용자");
        final User secondUser = saveUser("둘째 닉네임 사용자");

        updateNickname(accessToken(firstUser), "중복확인01")
                .andExpect(status().isNoContent());
        updateNickname(accessToken(secondUser), "기존닉네임")
                .andExpect(status().isNoContent());

        updateNickname(accessToken(secondUser), "중복확인01")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER_ERROR"));

        assertThat(userJpaRepository.findById(secondUser.id()).orElseThrow().nickname())
                .isEqualTo("기존닉네임");
    }

    @Test
    void 존재하지_않는_사용자는_404를_응답한다() throws Exception {
        final String token = jwtTokenProvider.createAccessToken(999999L, UserRole.USER);

        updateNickname(token, "없는사용자01")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NO_RESOURCE_ERROR"));
    }

    @Test
    void 비로그인_사용자는_401을_응답한다() throws Exception {
        updateNickname(null, "장보고01")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void 닉네임_저장_API를_OpenAPI에_노출한다() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/users/me'].patch").exists())
                .andExpect(jsonPath("$.paths['/api/v1/users/me'].patch.responses['400']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/users/me'].patch.responses['204']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/users/me'].patch.responses['401']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/users/me'].patch.responses['409']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/users/me'].patch.security[0].bearerAuth").isArray())
                .andExpect(jsonPath("$.components.schemas.UpdateUserNicknameRequest.properties.nickname").exists());
    }

    private org.springframework.test.web.servlet.ResultActions updateNickname(
            final String token, final String nickname) throws Exception {
        final MockHttpServletRequestBuilder request = patch(PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nickname\":\"%s\"}".formatted(nickname));
        if (token != null) {
            request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        return mockMvc.perform(request);
    }

    private User saveUser(final String name) {
        return userJpaRepository.save(User.oauth(
                ProviderType.KAKAO,
                UUID.randomUUID().toString(),
                UUID.randomUUID() + "@example.com",
                name));
    }

    private String accessToken(final User user) {
        return jwtTokenProvider.createAccessToken(user.id(), user.role());
    }
}
