package com.example.demo.auth.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.auth.domain.ProviderType;
import com.example.demo.auth.domain.User;
import com.example.demo.auth.infrastructure.persistence.UserJpaRepository;
import com.example.demo.auth.infrastructure.token.JwtTokenProvider;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class UserNicknameE2ETest {

    private static final String POSTGRES_IMAGE =
            "postgres:17-alpine@sha256:742f40ea20b9ff2ff31db5458d127452988a2164df9e17441e191f3b72252193";
    private static final String PATH = "/api/v1/users/me";

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
                    DockerImageName.parse(POSTGRES_IMAGE).asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("app")
            .withUsername("app")
            .withPassword("test-password");

    private final MockMvc mockMvc;
    private final UserJpaRepository userJpaRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void configurePostgres(final DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> true);
    }

    @Autowired
    UserNicknameE2ETest(
            final MockMvc mockMvc,
            final UserJpaRepository userJpaRepository,
            final JwtTokenProvider jwtTokenProvider,
            final JdbcTemplate jdbcTemplate) {
        this.mockMvc = mockMvc;
        this.userJpaRepository = userJpaRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Test
    void ROLE_USER는_앞뒤_공백을_제거한_nickname을_저장하고_204를_응답하며_지역_데이터를_변경하지_않는다() throws Exception {
        final User user = saveUser("닉네임 저장 사용자");
        final int regionsBefore = rowCount("regions");

        updateNickname(accessToken(user), "  장보고01  ")
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        assertThat(nicknameOf(user.id())).isEqualTo("장보고01");
        assertThat(rowCount("regions")).isEqualTo(regionsBefore);
    }

    @Test
    void 앞뒤_공백만_있는_nickname과_형식에_맞지_않는_nickname은_400을_응답한다() throws Exception {
        final String token = accessToken(saveUser("닉네임 유효성 사용자"));

        updateNickname(token, "  ").andExpect(status().isBadRequest());
        updateNickname(token, "a").andExpect(status().isBadRequest());
        updateNickname(token, "nickname-too-long").andExpect(status().isBadRequest());
        updateNickname(token, "닉네임!").andExpect(status().isBadRequest());
    }

    @Test
    void 이미_저장된_nickname은_409를_응답하고_기존_nickname을_보존한다() throws Exception {
        final User firstUser = saveUser("첫 사용자");
        final User secondUser = saveUser("두번째 사용자");

        updateNickname(accessToken(firstUser), "중복닉네임").andExpect(status().isNoContent());
        updateNickname(accessToken(secondUser), "두번째닉네임").andExpect(status().isNoContent());

        updateNickname(accessToken(secondUser), "중복닉네임").andExpect(status().isConflict());

        assertThat(nicknameOf(secondUser.id())).isEqualTo("두번째닉네임");
    }

    @Test
    void 비로그인_사용자는_nickname을_저장할_수_없고_401을_응답한다() throws Exception {
        mockMvc.perform(patch(PATH).contentType(MediaType.APPLICATION_JSON).content("{\"nickname\":\"장보고01\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void nickname_저장_경로와_응답_계약을_OpenAPI에_노출한다() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/users/me'].patch").exists())
                .andExpect(jsonPath("$.paths['/api/v1/users/me'].patch.requestBody").exists())
                .andExpect(jsonPath("$.paths['/api/v1/users/me'].patch.responses['204']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/users/me'].patch.responses['400']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/users/me'].patch.responses['401']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/users/me'].patch.responses['409']").exists())
                .andExpect(jsonPath("$.components.schemas.UpdateUserNicknameRequest.properties.nickname").exists());
    }

    private org.springframework.test.web.servlet.ResultActions updateNickname(final String token, final String nickname)
            throws Exception {
        return mockMvc.perform(patch(PATH)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nickname\":\"%s\"}".formatted(nickname)));
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

    private String nicknameOf(final Long userId) {
        return jdbcTemplate.queryForObject("SELECT nickname FROM users WHERE id = ?", String.class, userId);
    }

    private int rowCount(final String tableName) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Integer.class);
    }

    private String bearer(final String token) {
        return "Bearer " + token;
    }
}
