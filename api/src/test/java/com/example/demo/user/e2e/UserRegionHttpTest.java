package com.example.demo.user.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.auth.domain.ProviderType;
import com.example.demo.auth.domain.User;
import com.example.demo.auth.infrastructure.persistence.UserJpaRepository;
import com.example.demo.auth.infrastructure.token.JwtTokenProvider;
import com.example.demo.common.exception.ErrorType;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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
class UserRegionHttpTest {

    private static final String POSTGRES_IMAGE =
            "postgres:17-alpine@sha256:742f40ea20b9ff2ff31db5458d127452988a2164df9e17441e191f3b72252193";
    private static final String PATH = "/api/v1/users/me/regions";

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
    UserRegionHttpTest(
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
    void ROLE_USER가_관심_지역을_추가하면_204와_법정동_코드를_저장한다() throws Exception {
        final User user = saveUser("관심 지역 사용자");

        add(accessToken(user), "1121510100")
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        final var saved = jdbcTemplate.queryForMap(
                "SELECT region_id, is_current FROM user_regions WHERE user_id = ?", user.id());
        assertThat(saved)
                .containsEntry("region_id", "1121510100")
                .containsEntry("is_current", false);
        assertThat(regionName("1121510100")).isEqualTo("서울특별시 광진구 중곡동");
    }

    @Test
    void 동일한_관심_지역을_중복_추가하면_409와_오류_코드를_응답한다() throws Exception {
        final User user = saveUser("중복 관심 지역 사용자");
        final String token = accessToken(user);

        add(token, "1121510100").andExpect(status().isNoContent());
        add(token, "1121510100")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ErrorType.DUPLICATE_USER_REGION_ERROR.name()));

        assertThat(regionCount(user.id())).isEqualTo(1);
    }

    @Test
    void 네_번째_관심_지역은_409와_최대_개수_오류를_응답한다() throws Exception {
        final User user = saveUser("관심 지역 한도 사용자");
        final String token = accessToken(user);

        add(token, "1121510100").andExpect(status().isNoContent());
        add(token, "1121510200").andExpect(status().isNoContent());
        add(token, "1121510300").andExpect(status().isNoContent());
        add(token, "1121510400")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ErrorType.USER_REGION_LIMIT_EXCEEDED_ERROR.name()));

        assertThat(regionCount(user.id())).isEqualTo(3);
    }

    @Test
    void 동시에_네_관심_지역을_추가해도_최대_세_개를_넘지_않는다() throws Exception {
        final User user = saveUser("동시 관심 지역 한도 사용자");
        final String token = accessToken(user);
        final ExecutorService executor = Executors.newFixedThreadPool(4);
        final CountDownLatch ready = new CountDownLatch(4);
        final CountDownLatch start = new CountDownLatch(1);
        final List<String> regionIds = List.of(
                "1121510100", "1121510200", "1121510300", "1121510400");

        try {
            final List<Future<Integer>> responses = regionIds.stream()
                    .map(regionId -> executor.submit(() -> addConcurrently(
                            token, regionId, ready, start)))
                    .toList();
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            assertThat(responses.stream().map(this::statusOf).toList())
                    .containsExactlyInAnyOrder(204, 204, 204, 409);
            assertThat(regionCount(user.id())).isEqualTo(3);
        } finally {
            executor.shutdownNow();
        }
    }

    private int addConcurrently(
            final String token,
            final String regionId,
            final CountDownLatch ready,
            final CountDownLatch start) throws Exception {
        ready.countDown();
        start.await();
        return add(token, regionId).andReturn().getResponse().getStatus();
    }

    @Test
    void 없는_지역을_current로_설정하면_자동_추가하고_반복해도_중복되지_않는다() throws Exception {
        final User user = saveUser("현재 지역 멱등 사용자");
        final String token = accessToken(user);

        setCurrent(token, "1121510200").andExpect(status().isNoContent());
        setCurrent(token, "1121510200").andExpect(status().isNoContent());

        assertThat(regionCount(user.id())).isEqualTo(1);
        assertThat(currentRegionCount(user.id())).isEqualTo(1);
        assertThat(currentRegionId(user.id())).isEqualTo("1121510200");
    }

    @Test
    void 존재하지_않는_지역을_current로_설정하면_400과_공통_오류를_응답한다() throws Exception {
        final User user = saveUser("잘못된 현재 지역 사용자");

        setCurrent(accessToken(user), "9999999999")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorType.INVALID_PARAMETER_ERROR.name()));

        assertThat(regionCount(user.id())).isZero();
    }

    @Test
    void 네_번째_지역을_current로_설정하면_409와_최대_개수_오류를_응답한다() throws Exception {
        final User user = saveUser("현재 지역 한도 사용자");
        final String token = accessToken(user);

        add(token, "1121510100").andExpect(status().isNoContent());
        add(token, "1121510200").andExpect(status().isNoContent());
        add(token, "1121510300").andExpect(status().isNoContent());
        setCurrent(token, "1121510400")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ErrorType.USER_REGION_LIMIT_EXCEEDED_ERROR.name()));

        assertThat(regionCount(user.id())).isEqualTo(3);
    }

    @Test
    void current를_변경하면_기존_지역은_해제되고_새_지역만_current가_된다() throws Exception {
        final User user = saveUser("현재 지역 변경 사용자");
        final String token = accessToken(user);

        add(token, "1121510100").andExpect(status().isNoContent());
        add(token, "1121510200").andExpect(status().isNoContent());
        setCurrent(token, "1121510100").andExpect(status().isNoContent());
        setCurrent(token, "1121510200").andExpect(status().isNoContent());

        assertThat(currentRegionCount(user.id())).isEqualTo(1);
        assertThat(currentRegionId(user.id())).isEqualTo("1121510200");
    }

    @Test
    void current_변경은_다른_사용자의_관심_지역을_변경하지_않는다() throws Exception {
        final User firstUser = saveUser("첫 관심 지역 사용자");
        final User secondUser = saveUser("둘째 관심 지역 사용자");

        final String firstToken = accessToken(firstUser);
        add(firstToken, "1121510100").andExpect(status().isNoContent());
        setCurrent(firstToken, "1121510100").andExpect(status().isNoContent());
        setCurrent(accessToken(secondUser), "1121510200").andExpect(status().isNoContent());

        assertThat(regionCount(firstUser.id())).isEqualTo(1);
        assertThat(currentRegionId(firstUser.id())).isEqualTo("1121510100");
        assertThat(currentRegionId(secondUser.id())).isEqualTo("1121510200");
    }

    @Test
    void 존재하지_않는_법정동_코드는_400과_공통_오류를_응답한다() throws Exception {
        final User user = saveUser("잘못된 지역 사용자");

        add(accessToken(user), "9999999999")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorType.INVALID_PARAMETER_ERROR.name()));

        assertThat(regionCount(user.id())).isZero();
    }

    @Test
    void 비로그인과_잘못된_JWT는_401을_응답한다() throws Exception {
        mockMvc.perform(post(PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"regionId\":\"1121510100\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorType.UNAUTHORIZED.name()));

        mockMvc.perform(post(PATH)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"regionId\":\"1121510100\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorType.INVALID_TOKEN.name()));

        mockMvc.perform(put(PATH + "/1121510100/current"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorType.UNAUTHORIZED.name()));

        mockMvc.perform(put(PATH + "/1121510100/current")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorType.INVALID_TOKEN.name()));
    }

    @Test
    void ROLE_GUEST는_관심_지역을_변경할_수_없고_403을_응답한다() throws Exception {
        mockMvc.perform(post(PATH)
                        .with(user("guest").roles("GUEST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"regionId\":\"1121510100\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorType.FORBIDDEN.name()));

        mockMvc.perform(put(PATH + "/1121510100/current")
                        .with(user("guest").roles("GUEST")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorType.FORBIDDEN.name()));
    }

    @Test
    void 사용자_관심_지역_쓰기_경로와_상태를_OpenAPI에_노출한다() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/users/me/regions'].post.responses['204']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/users/me/regions'].post.responses['400']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/users/me/regions'].post.responses['401']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/users/me/regions'].post.responses['403']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/users/me/regions'].post.responses['404']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/users/me/regions'].post.responses['409']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/users/me/regions/{regionId}/current'].put.responses['204']")
                        .exists())
                .andExpect(jsonPath("$.paths['/api/v1/users/me/regions/{regionId}/current'].put.responses['400']")
                        .exists())
                .andExpect(jsonPath("$.paths['/api/v1/users/me/regions/{regionId}/current'].put.responses['401']")
                        .exists())
                .andExpect(jsonPath("$.paths['/api/v1/users/me/regions/{regionId}/current'].put.responses['403']")
                        .exists())
                .andExpect(jsonPath("$.paths['/api/v1/users/me/regions/{regionId}/current'].put.responses['404']")
                        .exists())
                .andExpect(jsonPath("$.paths['/api/v1/users/me/regions/{regionId}/current'].put.responses['409']")
                        .exists())
                .andExpect(jsonPath("$.components.schemas.AddUserRegionRequest.properties.regionId").exists());
    }

    private org.springframework.test.web.servlet.ResultActions add(
            final String token, final String regionId) throws Exception {
        return mockMvc.perform(post(PATH)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"regionId\":\"%s\"}".formatted(regionId)));
    }

    private org.springframework.test.web.servlet.ResultActions setCurrent(
            final String token, final String regionId) throws Exception {
        return mockMvc.perform(put(PATH + "/" + regionId + "/current")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)));
    }

    private User saveUser(final String name) {
        return userJpaRepository.saveAndFlush(User.oauth(
                ProviderType.KAKAO,
                UUID.randomUUID().toString(),
                UUID.randomUUID() + "@example.com",
                name));
    }

    private String accessToken(final User user) {
        return jwtTokenProvider.createAccessToken(user.id(), user.role());
    }

    private int regionCount(final Long userId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_regions WHERE user_id = ?", Integer.class, userId);
    }

    private int currentRegionCount(final Long userId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_regions WHERE user_id = ? AND is_current = TRUE",
                Integer.class,
                userId);
    }

    private String currentRegionId(final Long userId) {
        return jdbcTemplate.queryForObject(
                "SELECT region_id FROM user_regions WHERE user_id = ? AND is_current = TRUE",
                String.class,
                userId);
    }

    private String regionName(final String regionId) {
        return jdbcTemplate.queryForObject(
                "SELECT region_name FROM regions WHERE region_id = ?", String.class, regionId);
    }

    private int statusOf(final Future<Integer> response) {
        try {
            return response.get(10, TimeUnit.SECONDS);
        } catch (final InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError("동시 요청 결과를 기다리는 중 인터럽트되었습니다.", exception);
        } catch (final Exception exception) {
            throw new AssertionError("동시 요청 결과를 읽지 못했습니다.", exception);
        }
    }

    private String bearer(final String token) {
        return "Bearer " + token;
    }
}
