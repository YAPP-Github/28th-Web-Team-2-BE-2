package com.example.demo.user.e2e;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.auth.domain.ProviderType;
import com.example.demo.auth.domain.User;
import com.example.demo.auth.domain.UserRole;
import com.example.demo.auth.infrastructure.persistence.UserJpaRepository;
import com.example.demo.auth.infrastructure.token.JwtTokenProvider;
import com.example.demo.common.exception.ErrorType;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class UserMeHttpTest {

    private static final String PATH = "/api/v1/users/me";

    private final MockMvc mockMvc;
    private final UserJpaRepository userJpaRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    UserMeHttpTest(
            final MockMvc mockMvc,
            final UserJpaRepository userJpaRepository,
            final JwtTokenProvider jwtTokenProvider,
            final JdbcTemplate jdbcTemplate) {
        this.mockMvc = mockMvc;
        this.userJpaRepository = userJpaRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.jdbcTemplate = jdbcTemplate;
    }

    @BeforeEach
    void cleanRegions() {
        jdbcTemplate.update("DELETE FROM user_regions");
        jdbcTemplate.update("DELETE FROM regions");
    }

    @Test
    void 인증한_사용자의_닉네임과_현재_지역을_직접_응답하고_COMPLETED를_반환한다()
            throws Exception {
        final User user = saveUser("마이페이지 완료 사용자");
        user.changeNickname("장보고01");
        userJpaRepository.saveAndFlush(user);
        saveRegion("1111010100", "서울특별시 종로구 청운동");
        saveCurrentRegion(user, "1111010100");

        mockMvc.perform(get(PATH).header(HttpHeaders.AUTHORIZATION, bearer(accessToken(user))))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").doesNotExist())
                .andExpect(jsonPath("$.message").doesNotExist())
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.nickname").value("장보고01"))
                .andExpect(jsonPath("$.currentRegion.regionId").value("1111010100"))
                .andExpect(jsonPath("$.currentRegion.regionName").value("서울특별시 종로구 청운동"))
                .andExpect(jsonPath("$.onboardingStep").value("COMPLETED"));
    }

    @Test
    void 닉네임만_저장된_사용자는_REGION_단계를_반환한다() throws Exception {
        final User user = saveUser("마이페이지 지역 단계 사용자");
        user.changeNickname("장보고02");
        userJpaRepository.saveAndFlush(user);

        mockMvc.perform(get(PATH).header(HttpHeaders.AUTHORIZATION, bearer(accessToken(user))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("장보고02"))
                .andExpect(jsonPath("$.currentRegion").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.onboardingStep").value("REGION"));
    }

    @Test
    void 닉네임과_현재_지역이_없는_사용자는_NICKNAME_단계를_반환한다() throws Exception {
        final User user = saveUser("마이페이지 닉네임 단계 사용자");

        mockMvc.perform(get(PATH).header(HttpHeaders.AUTHORIZATION, bearer(accessToken(user))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.currentRegion").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.onboardingStep").value("NICKNAME"));
    }

    @Test
    void 현재_지역만_저장된_사용자도_NICKNAME_단계를_반환한다() throws Exception {
        final User user = saveUser("마이페이지 현재 지역 단계 사용자");
        saveRegion("1111010100", "서울특별시 종로구 청운동");
        saveCurrentRegion(user, "1111010100");

        mockMvc.perform(get(PATH).header(HttpHeaders.AUTHORIZATION, bearer(accessToken(user))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.currentRegion.regionId").value("1111010100"))
                .andExpect(jsonPath("$.onboardingStep").value("NICKNAME"));
    }

    @Test
    void 비로그인_요청은_401을_응답한다() throws Exception {
        mockMvc.perform(get(PATH))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorType.UNAUTHORIZED.name()));

        mockMvc.perform(get(PATH).header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorType.INVALID_TOKEN.name()));
    }

    @Test
    void ROLE_GUEST_요청은_403을_응답한다() throws Exception {
        mockMvc.perform(get(PATH).with(user("guest").roles("GUEST")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorType.FORBIDDEN.name()));
    }

    @Test
    void 존재하지_않는_JWT_사용자는_404를_응답한다() throws Exception {
        final String token = jwtTokenProvider.createAccessToken(999999L, UserRole.USER);

        mockMvc.perform(get(PATH).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorType.NO_RESOURCE_ERROR.name()));
    }

    @Test
    void JWT_principal에_따라_현재_사용자의_기본정보만_응답한다() throws Exception {
        final User firstUser = saveUser("첫 마이페이지 사용자");
        firstUser.changeNickname("첫사용자01");
        userJpaRepository.saveAndFlush(firstUser);
        final User secondUser = saveUser("둘째 마이페이지 사용자");
        secondUser.changeNickname("둘째사용자01");
        userJpaRepository.saveAndFlush(secondUser);

        mockMvc.perform(get(PATH).header(HttpHeaders.AUTHORIZATION, bearer(accessToken(firstUser))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("첫사용자01"))
                .andExpect(jsonPath("$.nickname").value(org.hamcrest.Matchers.not("둘째사용자01")));
    }

    @Test
    void 사용자_기본정보_조회_API를_직접_응답과_함께_OpenAPI에_노출한다() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/users/me'].get.responses['200']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/users/me'].get.responses['401']").exists())
                .andExpect(jsonPath("$.components.schemas.UserMeResponse.properties.nickname").exists())
                .andExpect(jsonPath("$.components.schemas.UserMeResponse.properties.currentRegion").exists())
                .andExpect(jsonPath("$.components.schemas.UserMeResponse.properties.onboardingStep").exists());
    }

    private User saveUser(final String name) {
        return userJpaRepository.saveAndFlush(User.oauth(
                ProviderType.KAKAO,
                UUID.randomUUID().toString(),
                UUID.randomUUID() + "@example.com",
                name));
    }

    private void saveRegion(final String regionId, final String regionName) {
        jdbcTemplate.update(
                "INSERT INTO regions (region_id, region_name) VALUES (?, ?)", regionId, regionName);
    }

    private void saveCurrentRegion(final User user, final String regionId) {
        jdbcTemplate.update(
                "INSERT INTO user_regions (user_id, region_id, is_current, created_at) "
                        + "VALUES (?, ?, TRUE, CURRENT_TIMESTAMP)",
                user.id(),
                regionId);
    }

    private String accessToken(final User user) {
        return jwtTokenProvider.createAccessToken(user.id(), user.role());
    }

    private String bearer(final String token) {
        return "Bearer " + token;
    }
}
