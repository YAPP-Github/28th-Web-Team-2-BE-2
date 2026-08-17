package com.example.demo.report.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.auth.domain.ProviderType;
import com.example.demo.auth.domain.User;
import com.example.demo.auth.infrastructure.persistence.UserJpaRepository;
import com.example.demo.auth.infrastructure.token.JwtTokenProvider;
import com.example.demo.item.domain.Item;
import com.example.demo.item.infrastructure.ItemJpaRepository;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class UserReportE2ETest {

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
    UserReportE2ETest(
            final MockMvc mockMvc,
            final UserJpaRepository userJpaRepository,
            final ItemJpaRepository itemJpaRepository,
            final JwtTokenProvider jwtTokenProvider,
            final JdbcTemplate jdbcTemplate) {
        this.mockMvc = mockMvc;
        this.userJpaRepository = userJpaRepository;
        this.itemJpaRepository = itemJpaRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.jdbcTemplate = jdbcTemplate;
    }

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM user_reports");
        jdbcTemplate.update("DELETE FROM stores");
        item = itemJpaRepository.findAll().getFirst();
    }

    @Test
    void 인증_사용자의_가격_제보는_장소_스냅샷과_report를_함께_저장한다() throws Exception {
        final User user = saveUser("제보 사용자");

        report(accessToken(user), item.id(), "16618597")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reportId").isNumber());

        final Long reportId = jdbcTemplate.queryForObject(
                "SELECT report_id FROM user_reports ORDER BY report_id DESC LIMIT 1", Long.class);
        final var snapshot = jdbcTemplate.queryForMap("""
                SELECT ur.item_id, ur.user_id, ur.price, ur.unit, ur.amount, ur.photo_url,
                       s.kakao_place_id, s.place_name, s.place_url, s.category_name,
                       s.address_name, s.road_address_name, s.phone, s.category_group_code,
                       s.category_group_name, s.longitude, s.latitude, s.distance
                  FROM user_reports ur
                  JOIN stores s ON s.store_id = ur.store_id
                 WHERE ur.report_id = ?
                """, reportId);

        assertThat(snapshot)
                .containsEntry("item_id", item.id())
                .containsEntry("user_id", user.id())
                .containsEntry("price", 3500)
                .containsEntry("unit", "kg")
                .containsEntry("photo_url", "https://images.example.com/reports/receipt.jpg")
                .containsEntry("kakao_place_id", "16618597")
                .containsEntry("place_name", "장생당약국")
                .containsEntry("place_url", "http://place.map.kakao.com/16618597")
                .containsEntry("category_name", "의료,건강 > 약국")
                .containsEntry("address_name", "서울 강남구 대치동 943-16")
                .containsEntry("road_address_name", "서울 강남구 테헤란로84길 17")
                .containsEntry("phone", "02-558-5476")
                .containsEntry("category_group_code", "PM9")
                .containsEntry("category_group_name", "약국")
                .containsEntry("distance", 10);
        assertThat(snapshot.get("amount").toString()).isEqualTo("1.250");
        assertThat(snapshot.get("longitude").toString()).isEqualTo("127.0589707834");
        assertThat(snapshot.get("latitude").toString()).isEqualTo("37.5060518881");
    }

    @Test
    void 같은_Kakao_장소를_여러_번_제보해도_store는_중복되지_않는다() throws Exception {
        final User user = saveUser("중복 장소 사용자");
        final String token = accessToken(user);

        report(token, item.id(), "same-place").andExpect(status().isCreated());
        report(token, item.id(), "same-place").andExpect(status().isCreated());

        assertThat(count("stores", "kakao_place_id", "same-place")).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user_reports", Integer.class)).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT store_id) FROM user_reports", Integer.class)).isEqualTo(1);
    }

    @Test
    void 동시에_같은_Kakao_장소를_제보해도_store는_하나만_생성된다() throws Exception {
        final User user = saveUser("동시성 장소 사용자");
        final String token = accessToken(user);
        final CountDownLatch ready = new CountDownLatch(2);
        final CountDownLatch start = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(2)) {
            final Future<Integer> first = executor.submit(() -> reportStatus(token, ready, start));
            final Future<Integer> second = executor.submit(() -> reportStatus(token, ready, start));
            ready.await();
            start.countDown();

            assertThat(first.get()).isEqualTo(201);
            assertThat(second.get()).isEqualTo(201);
        }

        assertThat(count("stores", "kakao_place_id", "concurrent-place")).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user_reports", Integer.class)).isEqualTo(2);
    }

    @Test
    void 존재하지_않는_품목은_매장과_제보를_저장하지_않고_404를_응답한다() throws Exception {
        final User user = saveUser("존재하지 않는 품목 사용자");

        report(accessToken(user), MISSING_ITEM_ID, "rollback-place")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NO_RESOURCE_ERROR"));

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM stores", Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user_reports", Integer.class)).isZero();
    }

    @Test
    void 가격_제보_OpenAPI에_생성_및_오류_응답을_노출한다() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/items/{itemId}/reports'].post.responses['201']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/items/{itemId}/reports'].post.responses['400']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/items/{itemId}/reports'].post.responses['401']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/items/{itemId}/reports'].post.responses['404']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/items/{itemId}/reports'].post.security[0].bearerAuth")
                        .isArray());
    }

    private ResultActions report(final String token, final Long itemId, final String kakaoPlaceId) throws Exception {
        return mockMvc.perform(post(reportPath(itemId))
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(reportBody(kakaoPlaceId)));
    }

    private int reportStatus(
            final String token, final CountDownLatch ready, final CountDownLatch start) throws Exception {
        ready.countDown();
        start.await();
        return report(token, item.id(), "concurrent-place").andReturn().getResponse().getStatus();
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

    private int count(final String table, final String column, final String value) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + table + " WHERE " + column + " = ?", Integer.class, value);
    }

    private String reportPath(final Long itemId) {
        return "/api/v1/items/" + itemId + "/reports";
    }

    private String bearer(final String token) {
        return "Bearer " + token;
    }

    private String reportBody(final String kakaoPlaceId) {
        return """
                {
                  "price": 3500,
                  "unit": "kg",
                  "amount": 1.25,
                  "store": {
                    "id": "%s",
                    "placeName": "장생당약국",
                    "placeUrl": "http://place.map.kakao.com/%s",
                    "categoryName": "의료,건강 > 약국",
                    "addressName": "서울 강남구 대치동 943-16",
                    "roadAddressName": "서울 강남구 테헤란로84길 17",
                    "phone": "02-558-5476",
                    "categoryGroupCode": "PM9",
                    "categoryGroupName": "약국",
                    "x": 127.0589707834,
                    "y": 37.5060518881,
                    "distance": 10
                  },
                  "photoUrl": "https://images.example.com/reports/receipt.jpg"
                }
                """.formatted(kakaoPlaceId, kakaoPlaceId);
    }
}
