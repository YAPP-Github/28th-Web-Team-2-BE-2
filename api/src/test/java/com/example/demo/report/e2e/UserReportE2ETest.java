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
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
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
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class UserReportE2ETest {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

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
    UserReportE2ETest(
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
        jdbcTemplate.update("DELETE FROM user_reports");
        jdbcTemplate.update("DELETE FROM stores");
        jdbcTemplate.update("UPDATE public_prices SET price_date = ?", LocalDate.now(SEOUL));
        item = itemJpaRepository.findAll().getFirst();
    }

    @Test
    void 인증_사용자의_가격_제보는_장소_스냅샷과_report를_함께_저장한다() throws Exception {
        final User user = saveUser("제보 사용자");

        reportWithPrice(accessToken(user), item.id(), "16618597", 4000)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("요청이 성공적으로 처리되었습니다."))
                .andExpect(jsonPath("$.data.reportId").isNumber())
                .andExpect(jsonPath("$.data.itemId").value(item.id()))
                .andExpect(jsonPath("$.data.storeId").isNumber())
                .andExpect(jsonPath("$.data.reportedAt").isNotEmpty());

        final Long reportId = jdbcTemplate.queryForObject(
                "SELECT report_id FROM user_reports ORDER BY report_id DESC LIMIT 1", Long.class);
        final var snapshot = jdbcTemplate.queryForMap("""
                SELECT ur.item_id, ur.user_id, ur.region_id, ur.report_type, ur.price, ur.unit, ur.amount,
                       ur.public_price_diff, ur.price_diff_rate,
                       ur.store_id, ur.photo_url,
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
                .containsEntry("region_id", "1121510100")
                .containsEntry("report_type", "PURCHASE")
                .containsEntry("price", 4000)
                .containsEntry("unit", "1kg")
                .containsEntry("public_price_diff", 500)
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
        assertThat(snapshot.get("amount").toString()).isEqualTo("1.000");
        assertThat(snapshot.get("price_diff_rate").toString()).isEqualTo("14.29");
        assertThat(snapshot.get("longitude").toString()).isEqualTo("127.0589707834");
        assertThat(snapshot.get("latitude").toString()).isEqualTo("37.5060518881");
    }

    @Test
    void 매장_없는_가격_제보도_지역과_제보_유형을_저장하고_store_id는_null이다() throws Exception {
        final User user = saveUser("매장 없는 제보 사용자");

        mockMvc.perform(post(reportPath(item.id()))
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken(user)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reportBodyWithoutStore()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("요청이 성공적으로 처리되었습니다."))
                .andExpect(jsonPath("$.data.reportId").isNumber())
                .andExpect(jsonPath("$.data.itemId").value(item.id()))
                .andExpect(jsonPath("$.data.storeId").value((Object) null))
                .andExpect(jsonPath("$.data.reportedAt").isNotEmpty());

        final var snapshot = jdbcTemplate.queryForMap("""
                SELECT region_id, report_type, store_id
                  FROM user_reports
                 ORDER BY report_id DESC
                 LIMIT 1
                """);

        assertThat(snapshot)
                .containsEntry("region_id", "1121510100")
                .containsEntry("report_type", "OBSERVED")
                .containsEntry("store_id", null);
    }

    @Test
    void ROLE_GUEST는_가격_제보를_할_수_없다() throws Exception {
        final User user = saveUser("GUEST 제보 사용자");

        mockMvc.perform(post(reportPath(item.id()))
                        .header(HttpHeaders.AUTHORIZATION, bearer(guestAccessToken(user.id())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reportBodyWithoutStore()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 같은_Kakao_장소를_여러_번_제보해도_store는_중복되지_않는다() throws Exception {
        final User user = saveUser("중복 장소 사용자");
        final String token = accessToken(user);

        report(token, item.id(), "same-place").andExpect(status().isCreated());
        report(token, item.id(), "same-place")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_USER_REPORT_ERROR"));

        assertThat(count("stores", "kakao_place_id", "same-place")).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user_reports", Integer.class)).isEqualTo(1);
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

            assertThat(java.util.List.of(first.get(), second.get()))
                    .containsExactlyInAnyOrder(201, 409);
        }

        assertThat(count("stores", "kakao_place_id", "concurrent-place")).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user_reports", Integer.class)).isEqualTo(1);
    }

    @Test
    void 품목_기준_단위와_다른_제보는_저장하지_않고_400을_응답한다() throws Exception {
        final User user = saveUser("단위 오류 사용자");

        // 기준 단위가 "1kg"인 품목에 "개" — 한 개가 몇 kg인지는 품목마다 달라 환산할 수 없다.
        mockMvc.perform(post(reportPath(item.id()))
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken(user)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reportBodyWithUnit("개")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER_ERROR"));

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM stores", Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user_reports", Integer.class)).isZero();
    }

    @Test
    void 기준_단위_여러_개분_제보는_한_개분_가격으로_옮겨_저장한다() throws Exception {
        final User user = saveUser("수량 환산 사용자");

        // "2kg에 8000원" → 기준 단위가 "1kg"이므로 "1kg에 4000원"으로 저장한다.
        mockMvc.perform(post(reportPath(item.id()))
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken(user)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reportBody("amount-convert-place")
                                .replace("\"price\": 3500", "\"price\": 8000")
                                .replace("\"amount\": 1", "\"amount\": 2")))
                .andExpect(status().isCreated());

        final var saved = jdbcTemplate.queryForMap(
                "SELECT price, unit, amount FROM user_reports ORDER BY report_id DESC LIMIT 1");
        assertThat(saved).containsEntry("price", 4000).containsEntry("unit", "1kg");
        assertThat((BigDecimal) saved.get("amount")).isEqualByComparingTo("1");
    }

    @Test
    void 무게_단위가_다르면_기준_단위_한_개분_가격으로_환산한다() throws Exception {
        final User user = saveUser("무게 환산 사용자");

        // "500g에 3000원" → 1kg 기준이면 6000원이다.
        mockMvc.perform(post(reportPath(item.id()))
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken(user)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reportBody("gram-convert-place")
                                .replace("\"price\": 3500", "\"price\": 3000")
                                .replace("\"unit\": \"1kg\"", "\"unit\": \"g\"")
                                .replace("\"amount\": 1", "\"amount\": 500")))
                .andExpect(status().isCreated());

        final var saved = jdbcTemplate.queryForMap(
                "SELECT price, unit, amount FROM user_reports ORDER BY report_id DESC LIMIT 1");
        assertThat(saved).containsEntry("price", 6000).containsEntry("unit", "1kg");
        assertThat((BigDecimal) saved.get("amount")).isEqualByComparingTo("1");
    }

    @Test
    void 수량_접두사가_없는_단위는_기준_단위로_바꿔_저장한다() throws Exception {
        final User user = saveUser("단위 표기 사용자");

        // 클라이언트는 화면 표기를 그대로 보낸다("1kg" → "kg"). 같은 단위이므로 받아들이고,
        // 저장은 기준 단위 원본으로 통일한다 — 표기가 섞이면 기존 제보와 비교할 수 없다.
        mockMvc.perform(post(reportPath(item.id()))
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken(user)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reportBodyWithUnit("kg")))
                .andExpect(status().isCreated());

        assertThat(jdbcTemplate.queryForObject("SELECT unit FROM user_reports", String.class))
                .isEqualTo(item.defaultUnit());
    }

    @Test
    void 회원_삭제_후에도_제보는_보존되고_작성자만_익명화된다() throws Exception {
        final User user = saveUser("탈퇴 사용자");

        report(accessToken(user), item.id(), "anonymized-place")
                .andExpect(status().isCreated());

        userJpaRepository.deleteById(user.id());
        userJpaRepository.flush();

        final var report = jdbcTemplate.queryForMap(
                "SELECT user_id, item_id FROM user_reports ORDER BY report_id DESC LIMIT 1");
        assertThat(report)
                .containsEntry("user_id", null)
                .containsEntry("item_id", item.id());
    }

    @Test
    void 기존_storeId로도_매장을_연결해_제보할_수_있다() throws Exception {
        final User user = saveUser("storeId 제보 사용자");
        final Long storeId = saveStore("store-id-place");

        mockMvc.perform(post(reportPath(item.id()))
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken(user)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reportBodyWithStoreId(storeId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("요청이 성공적으로 처리되었습니다."))
                .andExpect(jsonPath("$.data.storeId").value(storeId));

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM stores", Integer.class)).isEqualTo(1);
    }

    @Test
    void 큰_공공가격_차이도_중복_409가_아니라_정상_저장된다() throws Exception {
        final User user = saveUser("큰 차이율 사용자");

        reportWithPrice(accessToken(user), item.id(), "wide-rate-place", 100000)
                .andExpect(status().isCreated());

        assertThat(jdbcTemplate.queryForObject(
                "SELECT price_diff_rate FROM user_reports ORDER BY report_id DESC LIMIT 1", BigDecimal.class))
                .isEqualByComparingTo("2757.14");
    }

    @Test
    void 양의_Integer_가격_전체_범위의_차이율을_저장한다() throws Exception {
        jdbcTemplate.update("""
                INSERT INTO public_prices (item_id, region_id, price, price_date)
                VALUES (?, '9999999999', 1, ?)
                """, item.id(), LocalDate.now(SEOUL));
        final User user = saveUser("Integer 최대 가격 사용자");

        reportWithPriceAndRegion(
                accessToken(user), item.id(), "integer-max-place", "9999999999", Integer.MAX_VALUE)
                .andExpect(status().isCreated());

        assertThat(jdbcTemplate.queryForObject(
                "SELECT price_diff_rate FROM user_reports ORDER BY report_id DESC LIMIT 1", BigDecimal.class))
                .isEqualByComparingTo("214748364600.00");
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
    void 공개_가게별_제보_조회는_가격_스냅샷_필터와_단위_조건을_적용한다() throws Exception {
        final User user = saveUser("가게 제보 조회 사용자");
        final User secondUser = saveUser("가게 제보 조회 사용자 2");
        final User thirdUser = saveUser("가게 제보 조회 사용자 3");
        final User fourthUser = saveUser("가게 제보 조회 사용자 4");
        final Long storeId = saveStore("store-report-query");
        jdbcTemplate.update("""
                INSERT INTO user_reports (
                    store_id, item_id, user_id, price, unit, amount, report_date,
                    public_price_diff, price_diff_rate, report_type
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'OBSERVED')
                """, storeId, item.id(), user.id(), 900, item.defaultUnit(), 1,
                LocalDate.now(SEOUL), -100, new BigDecimal("-10.00"));
        jdbcTemplate.update("""
                INSERT INTO user_reports (
                    store_id, item_id, user_id, price, unit, amount, report_date,
                    public_price_diff, price_diff_rate, report_type
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'OBSERVED')
                """, storeId, item.id(), secondUser.id(), 1100, item.defaultUnit(), 1,
                LocalDate.now(SEOUL), 100, new BigDecimal("10.00"));
        jdbcTemplate.update("""
                INSERT INTO user_reports (
                    store_id, item_id, user_id, price, unit, amount, report_date,
                    public_price_diff, price_diff_rate, report_type
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'OBSERVED')
                """, storeId, item.id(), thirdUser.id(), 1000, item.defaultUnit(), 1,
                LocalDate.now(SEOUL), 0, BigDecimal.ZERO);
        jdbcTemplate.update("""
                INSERT INTO user_reports (
                    store_id, item_id, user_id, price, unit, amount, report_date,
                    public_price_diff, price_diff_rate, report_type
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'OBSERVED')
                """, storeId, item.id(), fourthUser.id(), 1000, "2kg", 1,
                LocalDate.now(SEOUL), -1, new BigDecimal("-0.10"));

        mockMvc.perform(get("/api/v1/stores/{storeId}/reports", storeId)
                        .queryParam("filter", "CHEAP"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.storeId").value(storeId))
                .andExpect(jsonPath("$.data.summary.cheapCount").value(1))
                .andExpect(jsonPath("$.data.summary.expensiveCount").value(1))
                .andExpect(jsonPath("$.data.reports").isArray())
                .andExpect(jsonPath("$.data.reports.length()").value(1))
                .andExpect(jsonPath("$.data.reports[0].priceClassification").value("CHEAP"))
                .andExpect(jsonPath("$.data.reports[0].itemName").isNotEmpty())
                .andExpect(jsonPath("$.data.reports[0].userId").doesNotExist());

        mockMvc.perform(get("/api/v1/stores/{storeId}/reports", storeId)
                        .queryParam("filter", "EXPENSIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reports.length()").value(1))
                .andExpect(jsonPath("$.data.reports[0].priceClassification").value("EXPENSIVE"));

        mockMvc.perform(get("/api/v1/stores/{storeId}/reports", storeId)
                        .queryParam("filter", "ALL")
                        .queryParam("page", "0")
                        .queryParam("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reports.length()").value(2))
                .andExpect(jsonPath("$.data.reports[0].priceClassification").value("EQUAL"))
                .andExpect(jsonPath("$.data.hasNext").value(true));

        mockMvc.perform(get("/api/v1/stores/{storeId}/reports", storeId)
                        .queryParam("filter", "ALL")
                        .queryParam("page", "1")
                        .queryParam("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reports.length()").value(1))
                .andExpect(jsonPath("$.data.hasNext").value(false));

        mockMvc.perform(get("/api/v1/stores/{storeId}/reports", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NO_RESOURCE_ERROR"));
    }

    @Test
    void 가격_제보_OpenAPI에_생성_및_오류_응답을_노출한다() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/items/{itemId}/reports'].post.responses['201']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/items/{itemId}/reports'].post.responses['400']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/items/{itemId}/reports'].post.responses['401']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/items/{itemId}/reports'].post.responses['404']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/items/{itemId}/reports'].post.responses['201'].content['application/json'].schema.properties.code.example")
                        .value("SUCCESS"))
                .andExpect(jsonPath("$.paths['/api/v1/items/{itemId}/reports'].post.responses['201'].content['application/json'].schema.properties.message.example")
                        .value("요청이 성공적으로 처리되었습니다."))
                .andExpect(jsonPath("$.paths['/api/v1/items/{itemId}/reports'].post.responses['201'].content['application/json'].schema.properties.data.$ref")
                        .value("#/components/schemas/CreateUserReportResponse"))
                .andExpect(jsonPath("$.paths['/api/v1/items/{itemId}/reports'].post.responses['409']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/items/{itemId}/reports'].post.security[0].bearerAuth")
                        .isArray())
                .andExpect(jsonPath("$.paths['/api/v1/stores/{storeId}/reports'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/stores/{storeId}/reports'].get.responses['200']").exists())
                .andExpect(jsonPath("$.components.schemas.StoreReportsResponse.properties.summary").exists())
                .andExpect(jsonPath("$.components.schemas.CreateUserReportRequest.required")
                        .value(org.hamcrest.Matchers.hasItems("regionId", "reportType")))
                .andExpect(jsonPath("$.components.schemas.CreateUserReportRequest.required")
                        .value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("store"))))
                .andExpect(jsonPath("$.components.schemas.CreateUserReportResponse.required")
                        .value(org.hamcrest.Matchers.hasItems("reportId", "itemId", "reportedAt")));
    }

    private ResultActions report(final String token, final Long itemId, final String kakaoPlaceId) throws Exception {
        return reportWithPrice(token, itemId, kakaoPlaceId, 3500);
    }

    private ResultActions reportWithPrice(
            final String token, final Long itemId, final String kakaoPlaceId, final int price) throws Exception {
        return reportWithPriceAndRegion(token, itemId, kakaoPlaceId, "1121510100", price);
    }

    private ResultActions reportWithPriceAndRegion(
            final String token,
            final Long itemId,
            final String kakaoPlaceId,
            final String regionId,
            final int price) throws Exception {
        return mockMvc.perform(post(reportPath(itemId))
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(reportBody(kakaoPlaceId)
                        .replace("\"regionId\": \"1121510100\"", "\"regionId\": \"" + regionId + "\"")
                        .replace("\"price\": 3500", "\"price\": " + price)));
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

    private String guestAccessToken(final Long userId) {
        final Instant now = Instant.now();
        final SecretKey key = Keys.hmacShaKeyFor(accessSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("type", "access")
                .claim("role", "GUEST")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(30, ChronoUnit.MINUTES)))
                .signWith(key)
                .compact();
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
                  "regionId": "1121510100",
                  "reportType": "PURCHASE",
                  "price": 3500,
                  "unit": "1kg",
                  "amount": 1,
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

    private String reportBodyWithoutStore() {
        return """
                {
                  "regionId": "1121510100",
                  "reportType": "OBSERVED",
                  "price": 3500,
                  "unit": "1kg",
                  "amount": 1.25,
                  "photoUrl": "https://images.example.com/reports/receipt.jpg"
                }
                """;
    }

    private String reportBodyWithUnit(final String unit) {
        return reportBody("invalid-unit-place").replace("\"unit\": \"1kg\"", "\"unit\": \"" + unit + "\"");
    }

    private String reportBodyWithStoreId(final Long storeId) {
        return """
                {
                  "regionId": "1121510100",
                  "reportType": "PURCHASE",
                  "storeId": %d,
                  "price": 3500,
                  "unit": "1kg",
                  "amount": 1
                }
                """.formatted(storeId);
    }

    private Long saveStore(final String kakaoPlaceId) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO stores (kakao_place_id, place_name, address_name)
                VALUES (?, '제보 매장', '서울특별시 마포구')
                RETURNING store_id
                """, Long.class, kakaoPlaceId);
    }
}
