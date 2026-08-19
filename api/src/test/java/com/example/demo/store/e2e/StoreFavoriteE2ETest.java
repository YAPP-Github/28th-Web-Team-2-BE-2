package com.example.demo.store.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.auth.domain.ProviderType;
import com.example.demo.auth.domain.User;
import com.example.demo.auth.infrastructure.persistence.UserJpaRepository;
import com.example.demo.auth.infrastructure.token.JwtTokenProvider;
import com.example.demo.report.domain.Store;
import com.example.demo.store.infrastructure.persistence.StoreJpaRepository;
import java.util.UUID;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
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
class StoreFavoriteE2ETest {

    private static final String POSTGRES_IMAGE =
            "postgres:17-alpine@sha256:742f40ea20b9ff2ff31db5458d127452988a2164df9e17441e191f3b72252193";
    private static final long MISSING_STORE_ID = Long.MAX_VALUE;

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
                    DockerImageName.parse(POSTGRES_IMAGE).asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("app")
            .withUsername("app")
            .withPassword("test-password");

    private final MockMvc mockMvc;
    private final UserJpaRepository userJpaRepository;
    private final StoreJpaRepository storeJpaRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final JdbcTemplate jdbcTemplate;
    private Store store;

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
    StoreFavoriteE2ETest(
            final MockMvc mockMvc,
            final UserJpaRepository userJpaRepository,
            final StoreJpaRepository storeJpaRepository,
            final JwtTokenProvider jwtTokenProvider,
            final JdbcTemplate jdbcTemplate) {
        this.mockMvc = mockMvc;
        this.userJpaRepository = userJpaRepository;
        this.storeJpaRepository = storeJpaRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.jdbcTemplate = jdbcTemplate;
    }

    @BeforeEach
    void setUp() {
        store = storeJpaRepository.findAll().stream().findFirst().orElseGet(() -> storeJpaRepository.save(
                new Store(
                        "test-store-favorite",
                        "테스트 가게",
                        "https://example.com/store",
                        null,
                        "서울시 테스트구",
                        "서울시 테스트구 테스트로 1",
                        "02-1234-5678",
                        null,
                        null,
                        BigDecimal.valueOf(127.0),
                        BigDecimal.valueOf(37.5),
                        100)));
    }

    @Test
    void 인증_사용자는_같은_가게를_여러_번_단골_등록해도_204와_단일_관계를_유지한다() throws Exception {
        final User user = saveUser("단골 멱등 사용자");
        final String token = accessToken(user);

        favorite(token, store.id()).andExpect(status().isNoContent()).andExpect(content().string(""));
        favorite(token, store.id()).andExpect(status().isNoContent()).andExpect(content().string(""));

        assertThat(storeFavoriteCount(user.id(), store.id())).isEqualTo(1);
    }

    @Test
    void 존재하지_않는_가게의_단골_등록은_404를_응답한다() throws Exception {
        favorite(accessToken(saveUser("미존재 가게 사용자")), MISSING_STORE_ID)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NO_RESOURCE_ERROR"));
    }

    @Test
    void 비로그인_단골_등록은_401을_응답한다() throws Exception {
        mockMvc.perform(put(favoritePath(store.id())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    private org.springframework.test.web.servlet.ResultActions favorite(
            final String token, final Long storeId) throws Exception {
        return mockMvc.perform(put(favoritePath(storeId))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    private String favoritePath(final Long storeId) {
        return "/api/v1/stores/" + storeId + "/favorite";
    }

    private long storeFavoriteCount(final Long userId, final Long storeId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM store_favorites WHERE user_id = ? AND store_id = ?",
                Long.class,
                userId,
                storeId);
    }

    private User saveUser(final String nickname) {
        final User user = User.oauth(ProviderType.KAKAO, UUID.randomUUID().toString(),
                nickname + "@example.com", nickname);
        user.changeNickname(nickname);
        return userJpaRepository.save(user);
    }

    private String accessToken(final User user) {
        return jwtTokenProvider.createAccessToken(user.id(), user.role());
    }
}
