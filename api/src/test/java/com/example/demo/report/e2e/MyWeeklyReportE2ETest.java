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
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class MyWeeklyReportE2ETest {

    private static final String PATH = "/api/v1/users/me/reports/weekly";
    private static final String REGION_ID = "1121510100";
    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

    private final MockMvc mockMvc;
    private final UserJpaRepository userJpaRepository;
    private final ItemJpaRepository itemJpaRepository;
    private final UserReportJpaRepository userReportJpaRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final JdbcTemplate jdbcTemplate;
    private final String accessSecret;
    private Long potatoId;
    private Long onionId;
    private LocalDate weekStart;

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
        weekStart = LocalDate.now(SERVICE_ZONE)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    @Test
    @DisplayName("주간 7일을 월요일부터 반환하고 제보한 날만 hasReported가 true다")
    void returnsSevenDaysFromMonday() throws Exception {
        final User me = saveUser("나");
        saveOn(me.id(), potatoId, ReportType.PURCHASE, weekStart);

        request(accessToken(me))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalReportedDays").value(1))
                .andExpect(jsonPath("$.data.dailyReports.length()").value(7))
                .andExpect(jsonPath("$.data.dailyReports[0].reportedAt").value(weekStart.toString()))
                .andExpect(jsonPath("$.data.dailyReports[0].hasReported").value(true))
                .andExpect(jsonPath("$.data.dailyReports[0].itemId").value(potatoId))
                .andExpect(jsonPath("$.data.dailyReports[0].itemName").value("감자"))
                .andExpect(jsonPath("$.data.dailyReports[6].reportedAt")
                        .value(weekStart.plusDays(6).toString()))
                .andExpect(jsonPath("$.data.dailyReports[6].hasReported").value(false))
                .andExpect(jsonPath("$.data.dailyReports[6].itemId").doesNotExist())
                .andExpect(jsonPath("$.data.dailyReports[6].itemName").doesNotExist());
    }

    @Test
    @DisplayName("PURCHASE와 OBSERVED가 모두 활동 일수에 반영되고 같은 날짜는 하루로 집계된다")
    void countsBothReportTypesOncePerDay() throws Exception {
        final User me = saveUser("나");
        saveOn(me.id(), potatoId, ReportType.PURCHASE, weekStart);
        saveOn(me.id(), onionId, ReportType.OBSERVED, weekStart);
        saveOn(me.id(), onionId, ReportType.OBSERVED, weekStart.plusDays(1));

        request(accessToken(me))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalReportedDays").value(2))
                .andExpect(jsonPath("$.data.dailyReports[0].hasReported").value(true))
                .andExpect(jsonPath("$.data.dailyReports[0].itemName").value("양파"))
                .andExpect(jsonPath("$.data.dailyReports[1].hasReported").value(true))
                .andExpect(jsonPath("$.data.dailyReports[2].hasReported").value(false));
    }

    @Test
    @DisplayName("지난 주 제보는 이번 주 집계에 들어가지 않는다")
    void excludesReportsOutsideTheWeek() throws Exception {
        final User me = saveUser("나");
        saveOn(me.id(), potatoId, ReportType.PURCHASE, weekStart.minusDays(1));
        saveOn(me.id(), potatoId, ReportType.PURCHASE, weekStart.plusDays(7));

        request(accessToken(me))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalReportedDays").value(0))
                .andExpect(jsonPath("$.data.dailyReports[*].hasReported")
                        .value(contains(false, false, false, false, false, false, false)));
    }

    @Test
    @DisplayName("다른 사용자의 제보는 섞이지 않는다")
    void excludesOtherUsersReports() throws Exception {
        final User me = saveUser("나");
        final User other = saveUser("남");
        saveOn(other.id(), potatoId, ReportType.PURCHASE, weekStart);

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
