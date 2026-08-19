package com.example.demo.report.e2e;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.item.domain.Item;
import com.example.demo.item.domain.ItemCategory;
import com.example.demo.item.infrastructure.ItemJpaRepository;
import com.example.demo.report.domain.ReportType;
import com.example.demo.report.domain.Store;
import com.example.demo.report.domain.UserReport;
import com.example.demo.store.infrastructure.persistence.StoreJpaRepository;
import com.example.demo.report.infrastructure.UserReportJpaRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class RegionItemReportE2ETest {

    private static final String REGION_ID = "1121510100";
    private static final String OTHER_REGION_ID = "1168010100";

    private final MockMvc mockMvc;
    private final ItemJpaRepository itemJpaRepository;
    private final StoreJpaRepository storeJpaRepository;
    private final UserReportJpaRepository userReportJpaRepository;
    private final JdbcTemplate jdbcTemplate;
    private Long potatoId;
    private Long onionId;
    private Long storeId;

    @Autowired
    RegionItemReportE2ETest(
            final MockMvc mockMvc,
            final ItemJpaRepository itemJpaRepository,
            final StoreJpaRepository storeJpaRepository,
            final UserReportJpaRepository userReportJpaRepository,
            final JdbcTemplate jdbcTemplate) {
        this.mockMvc = mockMvc;
        this.itemJpaRepository = itemJpaRepository;
        this.storeJpaRepository = storeJpaRepository;
        this.userReportJpaRepository = userReportJpaRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @BeforeEach
    void setUp() {
        userReportJpaRepository.deleteAll();
        storeJpaRepository.deleteAll();
        itemJpaRepository.deleteAll();
        final Item potato = itemJpaRepository.save(
                new Item("감자", "1kg", null, ItemCategory.ROOT_VEGETABLES));
        final Item onion = itemJpaRepository.save(
                new Item("양파", "1kg", null, ItemCategory.SEASONINGS));
        potatoId = potato.id();
        onionId = onion.id();
        storeId = storeJpaRepository.save(new Store(
                        "kakao-1", "행복마트", null, null, "서울 은평구", null, null, null, null, null, null, null))
                .id();
        save(potato.id(), storeId, 3000, "1kg", -500, new BigDecimal("-14.29"));
        save(potato.id(), null, 4000, "1kg", 500, new BigDecimal("14.29"));
        save(potato.id(), storeId, 3500, "1kg", 0, BigDecimal.ZERO);
        save(potato.id(), storeId, 2500, "1kg", null, null);
        save(potato.id(), storeId, 9900, "100g", -100, new BigDecimal("-3.00"));
        saveInRegion(potato.id(), OTHER_REGION_ID, 1111, "1kg");
    }

    private void save(
            final Long itemId,
            final Long storeId,
            final int price,
            final String unit,
            final Integer publicPriceDiff,
            final BigDecimal priceDiffRate) {
        userReportJpaRepository.save(new UserReport(
                REGION_ID, ReportType.PURCHASE, storeId, itemId, 1L, price, unit,
                new BigDecimal("1.000"), publicPriceDiff, priceDiffRate, null));
    }

    /** 제보 기준일은 도메인이 정하므로, 날짜별 정렬을 검증하려면 저장 후 옮긴다. */
    private void moveReportDate(final int price, final LocalDate reportDate) {
        jdbcTemplate.update(
                "UPDATE user_reports SET report_date = ? WHERE price = ?", reportDate, price);
    }

    private void saveInRegion(final Long itemId, final String regionId, final int price, final String unit) {
        userReportJpaRepository.save(new UserReport(
                regionId, ReportType.PURCHASE, null, itemId, 1L, price, unit,
                new BigDecimal("1.000"), null, null, null));
    }

    @Test
    @DisplayName("지역과 품목의 제보를 가격 오름차순으로 조회하고 저장된 비교 스냅샷을 그대로 반환한다")
    void returnsReportsSortedByPrice() throws Exception {
        mockMvc.perform(get(path(REGION_ID, potatoId)).param("sort", "PRICE_ASC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.regionId").value(REGION_ID))
                .andExpect(jsonPath("$.data.itemId").value(potatoId))
                .andExpect(jsonPath("$.data.totalCount").value(4))
                .andExpect(jsonPath("$.data.reports[*].price").value(contains(2500, 3000, 3500, 4000)))
                .andExpect(jsonPath("$.data.reports[1].priceGap").value(-500))
                .andExpect(jsonPath("$.data.reports[1].priceDiffRate").value(-14.29))
                .andExpect(jsonPath("$.data.reports[1].amount").value(1.0))
                .andExpect(jsonPath("$.data.reports[1].unit").value("1kg"))
                .andExpect(jsonPath("$.data.reports[1].storeName").value("행복마트"))
                .andExpect(jsonPath("$.data.hasNext").value(false));
    }

    @Test
    @DisplayName("priceGap 부호에 따라 CHEAP·EXPENSIVE·EQUAL로 분류하고 비교값이 없으면 null이다")
    void classifiesByPriceGapSign() throws Exception {
        mockMvc.perform(get(path(REGION_ID, potatoId)).param("sort", "PRICE_ASC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reports[0].classification").doesNotExist())
                .andExpect(jsonPath("$.data.reports[1].classification").value("CHEAP"))
                .andExpect(jsonPath("$.data.reports[2].classification").value("EQUAL"))
                .andExpect(jsonPath("$.data.reports[3].classification").value("EXPENSIVE"));
    }

    @Test
    @DisplayName("가게 없는 제보도 제보 당시 지역 기준으로 응답에 남고 storeId·storeName이 null이다")
    void keepsStorelessReport() throws Exception {
        mockMvc.perform(get(path(REGION_ID, potatoId)).param("sort", "PRICE_ASC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reports[3].price").value(4000))
                .andExpect(jsonPath("$.data.reports[3].storeId").doesNotExist())
                .andExpect(jsonPath("$.data.reports[3].storeName").doesNotExist());
    }

    @Test
    @DisplayName("품목 기준 단위와 다른 단위의 제보는 제외한다")
    void excludesReportsWithMismatchedUnit() throws Exception {
        mockMvc.perform(get(path(REGION_ID, potatoId)).param("sort", "PRICE_ASC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reports[*].unit")
                        .value(contains("1kg", "1kg", "1kg", "1kg")));
    }

    @Test
    @DisplayName("verificationCount는 v1에서 응답에 포함하지 않는다")
    void omitsVerificationCount() throws Exception {
        mockMvc.perform(get(path(REGION_ID, potatoId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reports[0].verificationCount").doesNotExist());
    }

    @Test
    @DisplayName("sort를 생략하면 제보 기준일 최신순으로 반환한다")
    void defaultsToLatestSort() throws Exception {
        final LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        moveReportDate(3000, today.minusDays(3));
        moveReportDate(4000, today.minusDays(1));
        moveReportDate(3500, today.minusDays(2));

        mockMvc.perform(get(path(REGION_ID, potatoId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reports[*].price").value(contains(2500, 4000, 3500, 3000)))
                .andExpect(jsonPath("$.data.reports[0].reportedDate").value(today.toString()))
                .andExpect(jsonPath("$.data.reports[3].reportedDate")
                        .value(today.minusDays(3).toString()));
    }

    @Test
    @DisplayName("제보 목록 API가 OpenAPI 문서에 노출된다")
    void exposesApiDocs() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                                "$.paths['/api/v1/regions/{regionId}/items/{itemId}/reports'].get")
                        .exists());
    }

    @Test
    @DisplayName("page와 size로 나눠 조회하고 hasNext가 정확하다")
    void paginatesReports() throws Exception {
        mockMvc.perform(get(path(REGION_ID, potatoId))
                        .param("sort", "PRICE_ASC")
                        .param("page", "0")
                        .param("size", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reports[*].price").value(contains(2500, 3000, 3500)))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(3))
                .andExpect(jsonPath("$.data.hasNext").value(true));

        mockMvc.perform(get(path(REGION_ID, potatoId))
                        .param("sort", "PRICE_ASC")
                        .param("page", "1")
                        .param("size", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reports[*].price").value(contains(4000)))
                .andExpect(jsonPath("$.data.hasNext").value(false));
    }

    @Test
    @DisplayName("제보가 없으면 오류가 아닌 빈 목록을 반환한다")
    void returnsEmptyListWhenNoReport() throws Exception {
        mockMvc.perform(get(path(REGION_ID, onionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(0))
                .andExpect(jsonPath("$.data.reports").isEmpty());
    }

    @Test
    @DisplayName("존재하지 않는 품목은 404, 잘못된 page·size·sort는 400이다")
    void rejectsInvalidRequest() throws Exception {
        mockMvc.perform(get(path(REGION_ID, 999_999L)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(path(REGION_ID, potatoId)).param("page", "-1"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(path(REGION_ID, potatoId)).param("size", "0"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(path(REGION_ID, potatoId)).param("sort", "CHEAPEST"))
                .andExpect(status().isBadRequest());
    }

    private String path(final String regionId, final Long itemId) {
        return "/api/v1/regions/" + regionId + "/items/" + itemId + "/reports";
    }
}
