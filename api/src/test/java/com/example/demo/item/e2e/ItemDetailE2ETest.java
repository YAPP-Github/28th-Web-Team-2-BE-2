package com.example.demo.item.e2e;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.auth.domain.ProviderType;
import com.example.demo.auth.domain.User;
import com.example.demo.auth.infrastructure.persistence.UserJpaRepository;
import com.example.demo.auth.infrastructure.token.JwtTokenProvider;
import com.example.demo.item.domain.Item;
import com.example.demo.item.domain.ItemCategory;
import com.example.demo.item.domain.OnlineChannel;
import com.example.demo.item.domain.OnlinePrice;
import com.example.demo.item.domain.PublicPrice;
import com.example.demo.item.infrastructure.ItemJpaRepository;
import com.example.demo.item.infrastructure.OnlineChannelJpaRepository;
import com.example.demo.item.infrastructure.OnlinePriceJpaRepository;
import com.example.demo.item.infrastructure.PublicPriceJpaRepository;
import com.example.demo.report.domain.ReportType;
import com.example.demo.report.domain.UserReport;
import com.example.demo.report.infrastructure.UserReportJpaRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;
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
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ItemDetailE2ETest {

    private static final String REGION_ID = "1121510100";
    private static final String OTHER_REGION_ID = "1168010100";

    private final MockMvc mockMvc;
    private final ItemJpaRepository itemJpaRepository;
    private final PublicPriceJpaRepository publicPriceJpaRepository;
    private final OnlinePriceJpaRepository onlinePriceJpaRepository;
    private final OnlineChannelJpaRepository onlineChannelJpaRepository;
    private final UserReportJpaRepository userReportJpaRepository;
    private final UserJpaRepository userJpaRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final JdbcTemplate jdbcTemplate;
    private final String accessSecret;

    @Autowired
    ItemDetailE2ETest(
            final MockMvc mockMvc,
            final ItemJpaRepository itemJpaRepository,
            final PublicPriceJpaRepository publicPriceJpaRepository,
            final OnlinePriceJpaRepository onlinePriceJpaRepository,
            final OnlineChannelJpaRepository onlineChannelJpaRepository,
            final UserReportJpaRepository userReportJpaRepository,
            final UserJpaRepository userJpaRepository,
            final JwtTokenProvider jwtTokenProvider,
            final JdbcTemplate jdbcTemplate,
            @Value("${jwt.access-secret}") final String accessSecret) {
        this.mockMvc = mockMvc;
        this.itemJpaRepository = itemJpaRepository;
        this.publicPriceJpaRepository = publicPriceJpaRepository;
        this.onlinePriceJpaRepository = onlinePriceJpaRepository;
        this.onlineChannelJpaRepository = onlineChannelJpaRepository;
        this.userReportJpaRepository = userReportJpaRepository;
        this.userJpaRepository = userJpaRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.jdbcTemplate = jdbcTemplate;
        this.accessSecret = accessSecret;
    }

    @BeforeEach
    void setUp() {
        userReportJpaRepository.deleteAll();
        onlinePriceJpaRepository.deleteAll();
        onlineChannelJpaRepository.deleteAll();
        jdbcTemplate.update("DELETE FROM item_favorites");
        publicPriceJpaRepository.deleteAll();
        itemJpaRepository.deleteAll();
        userJpaRepository.deleteAll();
    }

    @Test
    void 공개_품목_상세는_직접_응답으로_가격_제보_온라인_최저가를_조회한다() throws Exception {
        final Item item = saveItem("감자", "1kg");
        final LocalDate today = LocalDate.now();
        publicPriceJpaRepository.save(new PublicPrice(item.id(), REGION_ID, 1000, today.minusDays(1)));
        publicPriceJpaRepository.save(new PublicPrice(item.id(), REGION_ID, 1200, today));
        publicPriceJpaRepository.save(new PublicPrice(item.id(), REGION_ID, 1500, today));

        final User reportUser = saveUser("제보 사용자");
        final UserReport olderReport = saveReport(item, reportUser, REGION_ID, ReportType.PURCHASE, 3000);
        final UserReport latestPurchase = saveReport(item, reportUser, REGION_ID, ReportType.PURCHASE, 3300);
        final UserReport latestObserved = saveReport(item, reportUser, REGION_ID, ReportType.OBSERVED, 3500);
        setReportDate(olderReport, today.minusDays(1));
        setReportDate(latestPurchase, today);
        setReportDate(latestObserved, today);
        saveReport(item, reportUser, OTHER_REGION_ID, ReportType.OBSERVED, 9999);
        saveReportWithUnit(item, reportUser, REGION_ID, 8888, "2kg");

        final OnlineChannel channel = onlineChannelJpaRepository.save(new OnlineChannel("온라인몰"));
        onlinePriceJpaRepository.save(new OnlinePrice(
                item.id(), channel.id(), item.name(), "오래된 상품", 1000, 100, "https://example.test/stale", null,
                today.minusDays(1)));
        onlinePriceJpaRepository.save(new OnlinePrice(
                item.id(), channel.id(), item.name(), "최신 비대상 단위", 100, 200, "https://example.test/200", null,
                today));
        onlinePriceJpaRepository.save(new OnlinePrice(
                item.id(), channel.id(), item.name(), "최신 높은 가격", 6000, 100, "https://example.test/6000", null,
                today));
        onlinePriceJpaRepository.save(new OnlinePrice(
                item.id(), channel.id(), item.name(), "최신 최저 가격", 5500, 100, "https://example.test/5500", null,
                today));

        mockMvc.perform(detailRequest(item.id()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").doesNotExist())
                .andExpect(jsonPath("$.message").doesNotExist())
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.itemId").value(item.id()))
                .andExpect(jsonPath("$.itemName").value("감자"))
                .andExpect(jsonPath("$.itemImageUrl").value(nullValue()))
                .andExpect(jsonPath("$.defaultUnit").value("1kg"))
                .andExpect(jsonPath("$.isLiked").value(false))
                .andExpect(jsonPath("$.latestLocalReportPrice").value(3500))
                .andExpect(jsonPath("$.todayPublicPrice").value(1500))
                // 온라인 최저가는 100g 기준으로 저장되고(5500) 품목 기준 단위(1kg)로 환산해 내려간다
                .andExpect(jsonPath("$.onlineLowestPrice").value(55000))
                .andExpect(jsonPath("$.baseDate").value(today.toString()))
                .andExpect(jsonPath("$.priceGap").value(300))
                .andExpect(jsonPath("$.priceDiffRate").value(25.0));
    }

    @Test
    void itemId와_regionId를_검증하고_없는_품목은_404를_응답한다() throws Exception {
        mockMvc.perform(detailRequest(0L))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/items/1"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/items/1").queryParam("regionId", "   "))
                .andExpect(status().isBadRequest());

        mockMvc.perform(detailRequest(999999L))
                .andExpect(status().isNotFound());
    }

    @Test
    void 비로그인과_GUEST는_isLiked_false이고_ROLE_USER는_자신의_찜만_조회한다() throws Exception {
        final Item item = saveItem("양파", "1kg");
        final User currentUser = saveUser("현재 사용자");
        final User otherUser = saveUser("다른 사용자");
        addFavorite(currentUser.id(), item.id());

        mockMvc.perform(detailRequest(item.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isLiked").value(false));

        mockMvc.perform(detailRequest(item.id())
                        .header(HttpHeaders.AUTHORIZATION, bearer(guestAccessToken(currentUser.id()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isLiked").value(false));

        mockMvc.perform(detailRequest(item.id())
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken(currentUser))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isLiked").value(true));

        mockMvc.perform(detailRequest(item.id())
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken(otherUser))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isLiked").value(false));
    }

    @Test
    void 가격_제보_온라인_데이터가_없으면_상세_가격_필드는_null이다() throws Exception {
        final Item item = saveItem("양배추", "1포기");

        mockMvc.perform(detailRequest(item.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.latestLocalReportPrice").value(nullValue()))
                .andExpect(jsonPath("$.todayPublicPrice").value(nullValue()))
                .andExpect(jsonPath("$.onlineLowestPrice").value(nullValue()))
                .andExpect(jsonPath("$.baseDate").value(nullValue()))
                .andExpect(jsonPath("$.priceGap").value(nullValue()))
                .andExpect(jsonPath("$.priceDiffRate").value(nullValue()));
    }

    @Test
    void 무게로_환산할_수_없는_단위의_품목은_온라인_최저가가_null이다() throws Exception {
        // 이 응답에는 값마다 단위를 담을 자리가 없다. 100g 가격을 그대로 두면 1개 기준 금액과 나란히 놓인다.
        final LocalDate today = LocalDate.now();
        final Item watermelon = saveItem("수박", "1개");
        final OnlineChannel channel = onlineChannelJpaRepository.save(new OnlineChannel("온라인몰"));
        onlinePriceJpaRepository.save(new OnlinePrice(
                watermelon.id(), channel.id(), watermelon.name(), "수박 한 통", 450, 100,
                "https://example.test/watermelon", null, today));

        mockMvc.perform(detailRequest(watermelon.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.defaultUnit").value("1개"))
                .andExpect(jsonPath("$.onlineLowestPrice").value(nullValue()));
    }

    @Test
    void 품목_상세_경로_쿼리와_직접_응답_schema를_OpenAPI에_노출한다() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/items/{itemId}'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/items/{itemId}'].get.parameters[?(@.name == 'itemId')]")
                        .isNotEmpty())
                .andExpect(jsonPath("$.paths['/api/v1/items/{itemId}'].get.parameters[?(@.name == 'regionId')]")
                        .isNotEmpty())
                .andExpect(jsonPath("$.paths['/api/v1/items/{itemId}'].get.responses['200'].content['application/json'].schema.$ref")
                        .value("#/components/schemas/ItemDetailResponse"))
                .andExpect(jsonPath("$.components.schemas.ItemDetailResponse.properties.itemId").exists())
                .andExpect(jsonPath("$.components.schemas.ItemDetailResponse.properties.itemName").exists())
                .andExpect(jsonPath("$.components.schemas.ItemDetailResponse.properties.itemImageUrl").exists())
                .andExpect(jsonPath("$.components.schemas.ItemDetailResponse.properties.defaultUnit").exists())
                .andExpect(jsonPath("$.components.schemas.ItemDetailResponse.properties.isLiked").exists())
                .andExpect(jsonPath("$.components.schemas.ItemDetailResponse.properties.latestLocalReportPrice").exists())
                .andExpect(jsonPath("$.components.schemas.ItemDetailResponse.properties.todayPublicPrice").exists())
                .andExpect(jsonPath("$.components.schemas.ItemDetailResponse.properties.onlineLowestPrice").exists())
                .andExpect(jsonPath("$.components.schemas.ItemDetailResponse.properties.baseDate").exists())
                .andExpect(jsonPath("$.components.schemas.ItemDetailResponse.properties.priceGap").exists())
                .andExpect(jsonPath("$.components.schemas.ItemDetailResponse.properties.priceDiffRate").exists());
    }

    private Item saveItem(final String name, final String defaultUnit) {
        return itemJpaRepository.save(new Item(name, defaultUnit, null, ItemCategory.ROOT_VEGETABLES));
    }

    private User saveUser(final String name) {
        return userJpaRepository.save(User.oauth(
                ProviderType.KAKAO,
                UUID.randomUUID().toString(),
                UUID.randomUUID() + "@example.com",
                name));
    }

    private UserReport saveReport(
            final Item item, final User user, final String regionId, final ReportType reportType, final int price) {
        return userReportJpaRepository.save(new UserReport(
                regionId, reportType, null, item.id(), user.id(), price, item.defaultUnit(), BigDecimal.ONE, null));
    }

    private void saveReportWithUnit(
            final Item item, final User user, final String regionId, final int price, final String unit) {
        userReportJpaRepository.save(new UserReport(
                regionId, ReportType.PURCHASE, null, item.id(), user.id(), price, unit, BigDecimal.ONE, null));
    }

    private void setReportDate(final UserReport report, final LocalDate reportDate) {
        jdbcTemplate.update("UPDATE user_reports SET report_date = ? WHERE report_id = ?", reportDate, report.id());
    }

    private void addFavorite(final Long userId, final Long itemId) {
        jdbcTemplate.update("INSERT INTO item_favorites (user_id, item_id) VALUES (?, ?)", userId, itemId);
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

    private String bearer(final String token) {
        return "Bearer " + token;
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder detailRequest(final Long itemId) {
        return get("/api/v1/items/{itemId}", itemId).queryParam("regionId", REGION_ID);
    }
}
