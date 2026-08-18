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
import java.time.Instant;
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
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class MyReportE2ETest {

    private static final String PATH = "/api/v1/users/me/reports";
    private static final String REGION_ID = "1121510100";

    private final MockMvc mockMvc;
    private final UserJpaRepository userJpaRepository;
    private final ItemJpaRepository itemJpaRepository;
    private final UserReportJpaRepository userReportJpaRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final String accessSecret;
    private Long potatoId;
    private Long onionId;

    @Autowired
    MyReportE2ETest(
            final MockMvc mockMvc,
            final UserJpaRepository userJpaRepository,
            final ItemJpaRepository itemJpaRepository,
            final UserReportJpaRepository userReportJpaRepository,
            final JwtTokenProvider jwtTokenProvider,
            @Value("${jwt.access-secret}") final String accessSecret) {
        this.mockMvc = mockMvc;
        this.userJpaRepository = userJpaRepository;
        this.itemJpaRepository = itemJpaRepository;
        this.userReportJpaRepository = userReportJpaRepository;
        this.jwtTokenProvider = jwtTokenProvider;
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
    @DisplayName("내 제보만 최신순으로 페이지 조회하고 저장된 priceGap 스냅샷을 반환한다")
    void returnsOnlyMyReports() throws Exception {
        final User me = saveUser("나");
        final User other = saveUser("남");
        save(me.id(), potatoId, 3000, -500);
        save(me.id(), onionId, 2800, null);
        save(other.id(), potatoId, 9999, 100);

        request(accessToken(me))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.totalPages").value(1))
                .andExpect(jsonPath("$.data.reports[*].itemName").value(contains("양파", "감자")))
                .andExpect(jsonPath("$.data.reports[0].price").value(2800))
                .andExpect(jsonPath("$.data.reports[0].priceGap").doesNotExist())
                .andExpect(jsonPath("$.data.reports[1].priceGap").value(-500))
                .andExpect(jsonPath("$.data.reports[1].unit").value("1kg"))
                .andExpect(jsonPath("$.data.reports[1].regionId").value(REGION_ID))
                .andExpect(jsonPath("$.data.hasNext").value(false));
    }

    @Test
    @DisplayName("page와 size 경계에서 total과 hasNext가 일치한다")
    void paginates() throws Exception {
        final User me = saveUser("나");
        save(me.id(), potatoId, 1000, null);
        save(me.id(), potatoId, 2000, null);
        save(me.id(), potatoId, 3000, null);

        request(accessToken(me), "0", "2")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reports[*].price").value(contains(3000, 2000)))
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.data.totalPages").value(2))
                .andExpect(jsonPath("$.data.hasNext").value(true));

        request(accessToken(me), "1", "2")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reports[*].price").value(contains(1000)))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.hasNext").value(false));
    }

    @Test
    @DisplayName("제보가 없는 사용자는 오류가 아닌 빈 목록을 받는다")
    void returnsEmptyList() throws Exception {
        request(accessToken(saveUser("신규")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0))
                .andExpect(jsonPath("$.data.reports").isEmpty());
    }

    @Test
    @DisplayName("가게 없는 제보도 제보 당시 regionId로 조회된다")
    void keepsStorelessReport() throws Exception {
        final User me = saveUser("나");
        save(me.id(), potatoId, 3000, null);

        request(accessToken(me))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reports[0].regionId").value(REGION_ID));
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

    @Test
    @DisplayName("잘못된 page·size는 400이다")
    void rejectsInvalidPaging() throws Exception {
        final String token = accessToken(saveUser("나"));
        request(token, "-1", "10").andExpect(status().isBadRequest());
        request(token, "0", "0").andExpect(status().isBadRequest());
        request(token, "0", "101").andExpect(status().isBadRequest());
    }

    private ResultActions request(final String token) throws Exception {
        return mockMvc.perform(get(PATH).header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    private ResultActions request(final String token, final String page, final String size)
            throws Exception {
        return mockMvc.perform(get(PATH)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .param("page", page)
                .param("size", size));
    }

    private void save(
            final Long userId, final Long itemId, final int price, final Integer publicPriceDiff) {
        userReportJpaRepository.save(new UserReport(
                REGION_ID, ReportType.PURCHASE, null, itemId, userId, price, "1kg",
                new BigDecimal("1.000"), publicPriceDiff, null, null));
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
