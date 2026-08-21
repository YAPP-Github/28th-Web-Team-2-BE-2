package com.example.demo.store.e2e;

import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import com.example.demo.auth.domain.ProviderType;
import com.example.demo.auth.domain.User;
import com.example.demo.auth.infrastructure.persistence.UserJpaRepository;
import com.example.demo.auth.infrastructure.token.JwtTokenProvider;
import com.example.demo.report.domain.ReportType;
import com.example.demo.report.domain.Store;
import com.example.demo.report.domain.UserReport;
import com.example.demo.report.infrastructure.UserReportJpaRepository;
import com.example.demo.store.domain.StoreFavorite;
import com.example.demo.store.infrastructure.persistence.StoreFavoriteJpaRepository;
import com.example.demo.store.infrastructure.persistence.StoreJpaRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class StoreDetailE2ETest {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private final MockMvc mockMvc;
    private final StoreJpaRepository storeJpaRepository;
    private final StoreFavoriteJpaRepository storeFavoriteJpaRepository;
    private final UserReportJpaRepository userReportJpaRepository;
    private final UserJpaRepository userJpaRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    StoreDetailE2ETest(
            final MockMvc mockMvc,
            final StoreJpaRepository storeJpaRepository,
            final StoreFavoriteJpaRepository storeFavoriteJpaRepository,
            final UserReportJpaRepository userReportJpaRepository,
            final UserJpaRepository userJpaRepository,
            final JwtTokenProvider jwtTokenProvider,
            final JdbcTemplate jdbcTemplate) {
        this.mockMvc = mockMvc;
        this.storeJpaRepository = storeJpaRepository;
        this.storeFavoriteJpaRepository = storeFavoriteJpaRepository;
        this.userReportJpaRepository = userReportJpaRepository;
        this.userJpaRepository = userJpaRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.jdbcTemplate = jdbcTemplate;
    }

    @BeforeEach
    void setUp() {
        userReportJpaRepository.deleteAll();
        storeFavoriteJpaRepository.deleteAll();
        storeJpaRepository.deleteAll();
        userJpaRepository.deleteAll();
        jdbcTemplate.update(
                "MERGE INTO regions (region_id, region_name) KEY (region_id) VALUES (?, ?)",
                "1121510100", "서울특별시 광진구 중곡동");
    }

    @Test
    void 가게_상세는_찜과_최근_30일_제보를_집계한다() throws Exception {
        final Store store = saveStore();
        final User firstUser = saveUser("첫 번째 사용자");
        final User secondUser = saveUser("두 번째 사용자");
        final User reportUser = saveUser("제보 사용자");
        storeFavoriteJpaRepository.save(new StoreFavorite(firstUser.id(), store.id()));
        storeFavoriteJpaRepository.save(new StoreFavorite(secondUser.id(), store.id()));

        saveReport(store.id(), -100, firstUser.id());
        saveReport(store.id(), 200, secondUser.id());
        final UserReport oldReport = saveReport(store.id(), -300, reportUser.id());
        jdbcTemplate.update(
                "UPDATE user_reports SET report_date = ? WHERE report_id = ?",
                LocalDate.now(SEOUL).minusDays(30),
                oldReport.id());

        mockMvc.perform(get("/api/v1/stores/{storeId}", store.id())
                        .queryParam("latitude", "37.5088")
                        .queryParam("longitude", "127.0732"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.storeId").value(store.id()))
                .andExpect(jsonPath("$.data.storeName").value("장보고 마트"))
                .andExpect(jsonPath("$.data.address").value("서울 강남구 삼성동 123"))
                .andExpect(jsonPath("$.data.regionId").value("1121510100"))
                .andExpect(jsonPath("$.data.regionName").value("서울특별시 광진구 중곡동"))
                .andExpect(jsonPath("$.data.favoriteCount").value(2))
                .andExpect(jsonPath("$.data.cheapItemCount").value(1))
                .andExpect(jsonPath("$.data.expensiveItemCount").value(1))
                .andExpect(jsonPath("$.data.totalReportedItemCount").value(2))
                .andExpect(jsonPath("$.data.latestReportedDate").value(LocalDate.now(SEOUL).toString()))
                .andExpect(jsonPath("$.data.latestReportedAt").isNotEmpty())
                .andExpect(jsonPath("$.data.distance")
                        .value(allOf(greaterThanOrEqualTo(870), lessThanOrEqualTo(890))))
                .andExpect(jsonPath("$.data.walkTimeMinutes").value(nullValue()))
                .andExpect(jsonPath("$.data.businessHours").isArray())
                .andExpect(jsonPath("$.data.businessHours").isEmpty())
                .andExpect(jsonPath("$.data.openStatus").value("UNKNOWN"));
    }

    @Test
    void 제보가_없는_가게는_집계값을_0과_null로_반환한다() throws Exception {
        final Store store = saveStore();

        mockMvc.perform(get("/api/v1/stores/{storeId}", store.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.favoriteCount").value(0))
                .andExpect(jsonPath("$.data.cheapItemCount").value(0))
                .andExpect(jsonPath("$.data.expensiveItemCount").value(0))
                .andExpect(jsonPath("$.data.totalReportedItemCount").value(0))
                .andExpect(jsonPath("$.data.latestReportedDate").value(nullValue()))
                .andExpect(jsonPath("$.data.latestReportedAt").value(nullValue()))
                .andExpect(jsonPath("$.data.distance").value(nullValue()));
    }

    @Test
    void 저장된_이미지와_영업시간을_다시_상세_응답으로_반환한다() throws Exception {
        final Store store = saveStore();
        store.updateDetailFields(
                "https://cdn.example.com/images/store.jpg",
                "월 09:00 ~ 18:00\n화 휴무",
                "OPEN");
        storeJpaRepository.saveAndFlush(store);

        mockMvc.perform(get("/api/v1/stores/{storeId}", store.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.storeImageUrl")
                        .value("https://cdn.example.com/images/store.jpg"))
                .andExpect(jsonPath("$.data.businessHours[0]").value("월 09:00 ~ 18:00"))
                .andExpect(jsonPath("$.data.businessHours[1]").value("화 휴무"))
                .andExpect(jsonPath("$.data.openStatus").value("OPEN"));
    }

    @Test
    void 비로그인과_GUEST는_false이고_ROLE_USER는_본인_찜만_반환한다() throws Exception {
        final Store store = saveStore();
        final User currentUser = saveUser("현재 사용자");
        final User otherUser = saveUser("다른 사용자");
        storeFavoriteJpaRepository.save(new StoreFavorite(currentUser.id(), store.id()));

        mockMvc.perform(get("/api/v1/stores/{storeId}", store.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isLiked").value(false));

        mockMvc.perform(get("/api/v1/stores/{storeId}", store.id())
                        .with(user("guest").roles("GUEST")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isLiked").value(false));

        mockMvc.perform(get("/api/v1/stores/{storeId}", store.id())
                        .header(HttpHeaders.AUTHORIZATION, bearer(jwtTokenProvider.createAccessToken(
                                currentUser.id(), currentUser.role()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isLiked").value(true));

        mockMvc.perform(get("/api/v1/stores/{storeId}", store.id())
                        .header(HttpHeaders.AUTHORIZATION, bearer(jwtTokenProvider.createAccessToken(
                                otherUser.id(), otherUser.role()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isLiked").value(false));
    }

    @Test
    void 없는_가게는_404를_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/stores/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NO_RESOURCE_ERROR"));
    }

    @Test
    void 상세_조회_경로와_query_response_schema를_OpenAPI에_노출한다() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/stores/{storeId}'].get").exists())
                .andExpect(jsonPath(
                        "$.paths['/api/v1/stores/{storeId}'].get.parameters[?(@.name == 'storeId')]")
                        .isNotEmpty())
                .andExpect(jsonPath(
                        "$.paths['/api/v1/stores/{storeId}'].get.parameters[?(@.name == 'latitude')]")
                        .isNotEmpty())
                .andExpect(jsonPath(
                        "$.paths['/api/v1/stores/{storeId}'].get.parameters[?(@.name == 'longitude')]")
                        .isNotEmpty())
                .andExpect(jsonPath("$.paths['/api/v1/stores/{storeId}'].get.responses['401']")
                        .exists())
                .andExpect(jsonPath("$.components.schemas.StoreDetailResponse.properties.storeImageUrl")
                        .exists())
                .andExpect(jsonPath("$.components.schemas.StoreDetailResponse.properties.businessHours")
                        .exists())
                .andExpect(jsonPath("$.components.schemas.StoreDetailResponse.properties.openStatus")
                        .exists())
                .andExpect(jsonPath("$.components.schemas.StoreDetailResponse.properties.favoriteCount")
                        .exists())
                .andExpect(jsonPath("$.components.schemas.StoreDetailResponse.properties.latestReportedAt")
                        .exists());
    }

    private Store saveStore() {
        return storeJpaRepository.save(new Store(
                "detail-place",
                "장보고 마트",
                null,
                null,
                "서울 강남구 삼성동 123",
                "서울 강남구 테헤란로 123",
                null,
                null,
                null,
                new BigDecimal("127.0632"),
                new BigDecimal("37.5088"),
                null));
    }

    private UserReport saveReport(final Long storeId, final int publicPriceDiff, final Long userId) {
        return userReportJpaRepository.save(new UserReport(
                "1121510100",
                ReportType.OBSERVED,
                storeId,
                1L,
                userId,
                1000,
                "1kg",
                BigDecimal.ONE,
                publicPriceDiff,
                BigDecimal.valueOf(publicPriceDiff),
                null));
    }

    private User saveUser(final String name) {
        return userJpaRepository.save(User.oauth(
                ProviderType.KAKAO,
                UUID.randomUUID().toString(),
                UUID.randomUUID() + "@example.com",
                name));
    }

    private String bearer(final String token) {
        return "Bearer " + token;
    }
}
