package com.example.demo.report.e2e;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.auth.domain.ProviderType;
import com.example.demo.auth.domain.User;
import com.example.demo.auth.infrastructure.persistence.UserJpaRepository;
import com.example.demo.auth.infrastructure.token.JwtTokenProvider;
import com.example.demo.item.domain.Item;
import com.example.demo.item.domain.ItemCategory;
import com.example.demo.item.infrastructure.ItemJpaRepository;
import com.example.demo.report.domain.ReportType;
import com.example.demo.report.domain.UserReport;
import com.example.demo.report.infrastructure.UserReportJpaRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@Import(MyWeeklyReportE2ETest.FixedClockConfig.class)
class MyWeeklyReportE2ETest {

    /**
     * 주 경계를 서버 기본 시간대와 무관하게 고정한다.
     *
     * <p>고른 순간은 2026-08-17(월) 08:00 KST = 2026-08-16T23:00Z 다. 같은 순간을 UTC 로 해석하면
     * 2026-08-16(일)이 되어 주 시작이 2026-08-10 으로 한 주 밀린다. 즉 서비스 시간대를 쓰지 않으면 이 테스트가
     * 실패한다 — 기대값을 프로덕션 계산식으로 만들면 양쪽이 함께 틀려도 통과하는 동어반복이 된다.
     */
    @TestConfiguration
    static class FixedClockConfig {

        @Bean
        @Primary
        Clock fixedServiceClock() {
            return Clock.fixed(Instant.parse("2026-08-16T23:00:00Z"), ZoneId.of("Asia/Seoul"));
        }
    }

    private static final String PATH = "/api/v1/users/me/reports/weekly";
    private static final String REGION_ID = "1121510100";
    /** 위 고정 시각이 속한 주의 월요일. 프로덕션 계산식을 쓰지 않고 리터럴로 못박는다. */
    private static final LocalDate WEEK_START = LocalDate.of(2026, 8, 17);

    private final MockMvc mockMvc;
    private final UserJpaRepository userJpaRepository;
    private final ItemJpaRepository itemJpaRepository;
    private final UserReportJpaRepository userReportJpaRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final JdbcTemplate jdbcTemplate;
    private final String accessSecret;
    private Long potatoId;
    private Long onionId;

    @Autowired
    MyWeeklyReportE2ETest(
            final MockMvc mockMvc,
            final UserJpaRepository userJpaRepository,
            final ItemJpaRepository itemJpaRepository,
            final UserReportJpaRepository userReportJpaRepository,
            final JwtTokenProvider jwtTokenProvider,
            final JdbcTemplate jdbcTemplate,
            @Value("${jwt.access-secret}") final String accessSecret) {
        this.mockMvc = mockMvc;
        this.userJpaRepository = userJpaRepository;
        this.itemJpaRepository = itemJpaRepository;
        this.userReportJpaRepository = userReportJpaRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.jdbcTemplate = jdbcTemplate;
        this.accessSecret = accessSecret;
    }

    @BeforeEach
    void setUp() {
        userReportJpaRepository.deleteAll();
        itemJpaRepository.deleteAll();
        userJpaRepository.deleteAll();
        potatoId = itemJpaRepository.save(new Item("감자", "1kg", null, ItemCategory.ROOT_VEGETABLES)).id();
        onionId = itemJpaRepository.save(new Item("양파", "1kg", null, ItemCategory.SEASONINGS)).id();
    }

    @Test
    @DisplayName("주간 7일을 월요일부터 반환하고 제보한 날만 hasReported가 true다")
    void returnsSevenDaysFromMonday() throws Exception {
        final User me = saveUser("나");
        saveOn(me.id(), potatoId, ReportType.PURCHASE, WEEK_START);

        request(accessToken(me))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalReportedDays").value(1))
                .andExpect(jsonPath("$.data.dailyReports.length()").value(7))
                .andExpect(jsonPath("$.data.dailyReports[0].date").value(WEEK_START.toString()))
                .andExpect(jsonPath("$.data.dailyReports[0].hasReported").value(true))
                .andExpect(jsonPath("$.data.dailyReports[0].itemId").value(potatoId))
                .andExpect(jsonPath("$.data.dailyReports[0].itemName").value("감자"))
                .andExpect(jsonPath("$.data.dailyReports[6].date")
                        .value(WEEK_START.plusDays(6).toString()))
                .andExpect(jsonPath("$.data.dailyReports[6].hasReported").value(false))
                .andExpect(jsonPath("$.data.dailyReports[6].itemId").doesNotExist())
                .andExpect(jsonPath("$.data.dailyReports[6].itemName").doesNotExist());
    }

    @Test
    @DisplayName("같은 날짜는 하루로 집계하고 그날 가장 먼저 등록한 제보를 대표로 삼는다")
    void countsBothReportTypesOncePerDay() throws Exception {
        final User me = saveUser("나");
        saveOn(me.id(), potatoId, ReportType.PURCHASE, WEEK_START);
        saveOn(me.id(), onionId, ReportType.OBSERVED, WEEK_START);
        saveOn(me.id(), onionId, ReportType.OBSERVED, WEEK_START.plusDays(1));

        request(accessToken(me))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalReportedDays").value(2))
                .andExpect(jsonPath("$.data.dailyReports[0].hasReported").value(true))
                .andExpect(jsonPath("$.data.dailyReports[0].itemName").value("감자"))
                .andExpect(jsonPath("$.data.dailyReports[1].hasReported").value(true))
                .andExpect(jsonPath("$.data.dailyReports[2].hasReported").value(false));
    }

    @Test
    @DisplayName("지난 주 제보는 이번 주 집계에 들어가지 않는다")
    void excludesReportsOutsideTheWeek() throws Exception {
        final User me = saveUser("나");
        saveOn(me.id(), potatoId, ReportType.PURCHASE, WEEK_START.minusDays(1));
        saveOn(me.id(), potatoId, ReportType.PURCHASE, WEEK_START.plusDays(7));

        request(accessToken(me))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalReportedDays").value(0))
                .andExpect(jsonPath("$.data.dailyReports[*].hasReported")
                        .value(contains(false, false, false, false, false, false, false)));
    }

    @Test
    @DisplayName("주의 마지막 날(일요일) 제보도 이번 주에 포함된다")
    void includesLastDayOfWeek() throws Exception {
        final User me = saveUser("나");
        saveOn(me.id(), potatoId, ReportType.PURCHASE, WEEK_START.plusDays(6));

        request(accessToken(me))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalReportedDays").value(1))
                .andExpect(jsonPath("$.data.dailyReports[6].hasReported").value(true));
    }

    @Test
    @DisplayName("주간 제보 현황 API가 OpenAPI 문서에 노출된다")
    void exposesApiDocs() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/users/me/reports/weekly'].get").exists());
    }

    @Test
    @DisplayName("다른 사용자의 제보는 섞이지 않는다")
    void excludesOtherUsersReports() throws Exception {
        final User me = saveUser("나");
        final User other = saveUser("남");
        saveOn(other.id(), potatoId, ReportType.PURCHASE, WEEK_START);

        request(accessToken(me))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalReportedDays").value(0));
    }

    @Test
    @DisplayName("제보가 없는 사용자는 오류가 아닌 0건 결과를 받는다")
    void returnsZeroForUserWithoutReports() throws Exception {
        request(accessToken(saveUser("신규")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalReportedDays").value(0))
                .andExpect(jsonPath("$.data.dailyReports.length()").value(7));
    }

    @Test
    @DisplayName("토큰이 없거나 GUEST면 401로 거절된다")
    void rejectsUnauthenticated() throws Exception {
        mockMvc.perform(get(PATH))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        mockMvc.perform(get(PATH).header(HttpHeaders.AUTHORIZATION, "Bearer " + guestAccessToken()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
    }

    private ResultActions request(final String token) throws Exception {
        return mockMvc.perform(get(PATH).header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    private void saveOn(
            final Long userId, final Long itemId, final ReportType type, final LocalDate reportDate) {
        final UserReport saved = userReportJpaRepository.save(new UserReport(
                REGION_ID, type, null, itemId, userId, 3000, "1kg",
                new BigDecimal("1.000"), null, null, null));
        jdbcTemplate.update(
                "UPDATE user_reports SET report_date = ? WHERE report_id = ?", reportDate, saved.id());
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
}
