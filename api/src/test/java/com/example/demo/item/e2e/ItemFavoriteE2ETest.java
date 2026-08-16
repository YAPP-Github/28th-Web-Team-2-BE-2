package com.example.demo.item.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.auth.domain.ProviderType;
import com.example.demo.auth.domain.User;
import com.example.demo.auth.infrastructure.persistence.UserJpaRepository;
import com.example.demo.auth.infrastructure.token.JwtTokenProvider;
import com.example.demo.item.domain.Item;
import com.example.demo.item.infrastructure.ItemJpaRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
class ItemFavoriteE2ETest {

    private static final String POSTGRES_IMAGE =
            "postgres:17-alpine@sha256:742f40ea20b9ff2ff31db5458d127452988a2164df9e17441e191f3b72252193";
    private static final long MISSING_ITEM_ID = Long.MAX_VALUE;

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
                    DockerImageName.parse(POSTGRES_IMAGE).asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("app")
            .withUsername("app")
            .withPassword("test-password");

    private final MockMvc mockMvc;
    private final UserJpaRepository userJpaRepository;
    private final ItemJpaRepository itemJpaRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final JdbcTemplate jdbcTemplate;
    private final String accessSecret;
    private Item item;

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
    ItemFavoriteE2ETest(
            final MockMvc mockMvc,
            final UserJpaRepository userJpaRepository,
            final ItemJpaRepository itemJpaRepository,
            final JwtTokenProvider jwtTokenProvider,
            final JdbcTemplate jdbcTemplate,
            @Value("${jwt.access-secret}") final String accessSecret) {
        this.mockMvc = mockMvc;
        this.userJpaRepository = userJpaRepository;
        this.itemJpaRepository = itemJpaRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.jdbcTemplate = jdbcTemplate;
        this.accessSecret = accessSecret;
    }

    @BeforeEach
    void setUp() {
        item = itemJpaRepository.findAll().getFirst();
    }

    @Test
    void 인증_사용자는_같은_품목을_여러_번_찜해도_204와_단일_관계를_유지한다() throws Exception {
        final User user = saveUser("멱등 사용자");
        final String token = accessToken(user);

        favorite(token, item.id()).andExpect(status().isNoContent()).andExpect(content().string(""));
        favorite(token, item.id()).andExpect(status().isNoContent()).andExpect(content().string(""));

        assertThat(favoriteCount(user.id(), item.id())).isEqualTo(1);
    }

    @Test
    void 동시에_같은_품목을_찜해도_두_요청은_204이고_관계는_하나만_저장된다() throws Exception {
        final User user = saveUser("동시성 사용자");
        final String token = accessToken(user);
        final CountDownLatch ready = new CountDownLatch(2);
        final CountDownLatch start = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(2)) {
            final Future<Integer> first = executor.submit(() -> favoriteStatus(token, ready, start));
            final Future<Integer> second = executor.submit(() -> favoriteStatus(token, ready, start));
            ready.await();
            start.countDown();

            assertThat(first.get()).isEqualTo(204);
            assertThat(second.get()).isEqualTo(204);
        }
        assertThat(favoriteCount(user.id(), item.id())).isEqualTo(1);
    }

    @Test
    void 찜_삭제는_현재_사용자_관계만_제거하고_없는_관계도_204를_응답한다() throws Exception {
        final User currentUser = saveUser("현재 사용자");
        final User otherUser = saveUser("다른 사용자");
        final String currentToken = accessToken(currentUser);
        final String otherToken = accessToken(otherUser);
        favorite(currentToken, item.id()).andExpect(status().isNoContent());
        favorite(otherToken, item.id()).andExpect(status().isNoContent());

        mockMvc.perform(delete(favoritePath(item.id())).header(HttpHeaders.AUTHORIZATION, bearer(currentToken)))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        mockMvc.perform(delete(favoritePath(item.id())).header(HttpHeaders.AUTHORIZATION, bearer(currentToken)))
                .andExpect(status().isNoContent());

        assertThat(favoriteCount(currentUser.id(), item.id())).isZero();
        assertThat(favoriteCount(otherUser.id(), item.id())).isEqualTo(1);
    }

    @Test
    void 존재하지_않는_품목의_찜_추가와_삭제는_404를_응답한다() throws Exception {
        final String token = accessToken(saveUser("미존재 품목 사용자"));

        favorite(token, MISSING_ITEM_ID)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NO_RESOURCE_ERROR"));
        mockMvc.perform(delete(favoritePath(MISSING_ITEM_ID)).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NO_RESOURCE_ERROR"));
    }

    @Test
    void 비로그인과_ROLE_GUEST의_찜_추가와_삭제는_401을_응답한다() throws Exception {
        mockMvc.perform(put(favoritePath(item.id())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.data").value(nullValue()));
        mockMvc.perform(delete(favoritePath(item.id())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        final String guestToken = guestAccessToken();
        mockMvc.perform(put(favoritePath(item.id())).header(HttpHeaders.AUTHORIZATION, bearer(guestToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
        mockMvc.perform(delete(favoritePath(item.id())).header(HttpHeaders.AUTHORIZATION, bearer(guestToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
        mockMvc.perform(post("/api/samples")
                        .header(HttpHeaders.AUTHORIZATION, bearer(guestToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"GUEST는 저장할 수 없다\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorType").value("INVALID_TOKEN"));
    }

    @Test
    void 찜_추가와_삭제_계약을_OpenAPI에_노출한다() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/items/{itemId}/favorite'].put.responses['204']")
                        .exists())
                .andExpect(jsonPath("$.paths['/api/v1/items/{itemId}/favorite'].put.responses['401']")
                        .exists())
                .andExpect(jsonPath("$.paths['/api/v1/items/{itemId}/favorite'].put.responses['404']")
                        .exists())
                .andExpect(jsonPath("$.paths['/api/v1/items/{itemId}/favorite'].delete.responses['204']")
                        .exists())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type").value("http"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.bearerFormat").value("JWT"))
                .andExpect(jsonPath("$.paths['/api/v1/items/{itemId}/favorite'].put.security[0].bearerAuth")
                        .isArray())
                .andExpect(jsonPath("$.paths['/api/v1/items/{itemId}/favorite'].delete.security[0].bearerAuth")
                        .isArray());
    }

    private org.springframework.test.web.servlet.ResultActions favorite(final String token, final Long itemId)
            throws Exception {
        return mockMvc.perform(put(favoritePath(itemId)).header(HttpHeaders.AUTHORIZATION, bearer(token)));
    }

    private int favoriteStatus(
            final String token, final CountDownLatch ready, final CountDownLatch start) throws Exception {
        ready.countDown();
        start.await();
        return favorite(token, item.id()).andReturn().getResponse().getStatus();
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

    private String guestAccessToken() {
        final Instant now = Instant.now();
        final SecretKey key = Keys.hmacShaKeyFor(accessSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject("999999")
                .claim("type", "access")
                .claim("role", "GUEST")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(30, ChronoUnit.MINUTES)))
                .signWith(key)
                .compact();
    }

    private int favoriteCount(final Long userId, final Long itemId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM item_favorites WHERE user_id = ? AND item_id = ?",
                Integer.class,
                userId,
                itemId);
    }

    private String favoritePath(final Long itemId) {
        return "/api/v1/items/" + itemId + "/favorite";
    }

    private String bearer(final String token) {
        return "Bearer " + token;
    }
}
