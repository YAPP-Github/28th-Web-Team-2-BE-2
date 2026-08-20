package com.example.demo.report.e2e;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.item.domain.Item;
import com.example.demo.item.domain.ItemCategory;
import com.example.demo.item.domain.PublicPrice;
import com.example.demo.item.infrastructure.ItemJpaRepository;
import com.example.demo.item.infrastructure.PublicPriceJpaRepository;
import com.example.demo.report.domain.ReportType;
import com.example.demo.report.domain.Store;
import com.example.demo.report.domain.UserReport;
import com.example.demo.report.infrastructure.UserReportJpaRepository;
import com.example.demo.store.infrastructure.persistence.StoreJpaRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class RegionLowestPriceReportHttpTest {

    private static final String REGION_ID = "9999999999";
    private static final String REGION_NAME = "테스트 지역";
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private final MockMvc mockMvc;
    private final ItemJpaRepository itemJpaRepository;
    private final PublicPriceJpaRepository publicPriceJpaRepository;
    private final UserReportJpaRepository userReportJpaRepository;
    private final StoreJpaRepository storeJpaRepository;
    private final JdbcTemplate jdbcTemplate;
    private Item firstItem;
    private Item secondItem;

    @Autowired
    RegionLowestPriceReportHttpTest(
            final MockMvc mockMvc,
            final ItemJpaRepository itemJpaRepository,
            final PublicPriceJpaRepository publicPriceJpaRepository,
            final UserReportJpaRepository userReportJpaRepository,
            final StoreJpaRepository storeJpaRepository,
            final JdbcTemplate jdbcTemplate) {
        this.mockMvc = mockMvc;
        this.itemJpaRepository = itemJpaRepository;
        this.publicPriceJpaRepository = publicPriceJpaRepository;
        this.userReportJpaRepository = userReportJpaRepository;
        this.storeJpaRepository = storeJpaRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM user_reports WHERE region_id = ?", REGION_ID);
        jdbcTemplate.update("DELETE FROM public_prices WHERE region_id = ?", REGION_ID);
        ensureRegion();
        firstItem = saveItem("테스트 감자");
        secondItem = saveItem("테스트 양파");
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM user_reports WHERE region_id = ?", REGION_ID);
        jdbcTemplate.update("DELETE FROM public_prices WHERE region_id = ?", REGION_ID);
        if (firstItem != null) {
            itemJpaRepository.deleteById(firstItem.id());
        }
        if (secondItem != null) {
            itemJpaRepository.deleteById(secondItem.id());
        }
    }

    @Test
    void 최근_7일_품목별_최저가만_할인율_순으로_반환하고_storeless를_허용한다() throws Exception {
        final LocalDate today = LocalDate.now(SEOUL);
        final Store firstStore = saveStore("첫 번째 가게");
        final Store secondStore = saveStore("두 번째 가게");

        savePublicPrice(firstItem, today.minusDays(1), 1000);
        savePublicPrice(firstItem, today.minusDays(2), 2000);
        savePublicPrice(firstItem, today.minusDays(8), 1000);
        savePublicPrice(secondItem, today, 2000);

        saveReport(firstItem, firstStore, today.minusDays(1), 700, -300, "-30.00");
        saveReport(firstItem, secondStore, today.minusDays(2), 800, -1200, "-60.00");
        saveReport(firstItem, firstStore, today.minusDays(8), 100, -900, "-90.00");
        saveReport(secondItem, null, today, 1000, -1000, "-50.00");

        mockMvc.perform(get(path()).queryParam("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.regionName").value(REGION_NAME))
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[0].rank").value(1))
                .andExpect(jsonPath("$.data.items[0].itemId").value(secondItem.id().intValue()))
                .andExpect(jsonPath("$.data.items[0].price").value(1000))
                .andExpect(jsonPath("$.data.items[0].priceDiffRate").value(-50.00))
                .andExpect(jsonPath("$.data.items[0].storeId").value((Object) null))
                .andExpect(jsonPath("$.data.items[0].storeName").value((Object) null))
                .andExpect(jsonPath("$.data.items[1].rank").value(2))
                .andExpect(jsonPath("$.data.items[1].itemId").value(firstItem.id().intValue()))
                .andExpect(jsonPath("$.data.items[1].price").value(700))
                .andExpect(jsonPath("$.data.items[1].priceDiffRate").value(-30.00))
                .andExpect(jsonPath("$.data.items[1].storeId").value(firstStore.id().intValue()))
                .andExpect(jsonPath("$.data.items[1].storeName").value("첫 번째 가게"));
    }

    @Test
    void 공공가격보다_비싸거나_기준단위가_다른_제보는_제외한다() throws Exception {
        final LocalDate today = LocalDate.now(SEOUL);
        savePublicPrice(firstItem, today, 1000);
        saveReport(firstItem, null, today, 1200, 200, "20.00");
        saveReport(firstItem, null, today, 500, null, "-50.00");
        saveReport(firstItem, null, today, 500, -500, "-50.00", "2kg");

        mockMvc.perform(get(path()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isEmpty());
    }

    @Test
    void 제보가_없는_지역은_빈_목록을_반환하고_limit을_검증한다() throws Exception {
        mockMvc.perform(get(path()).queryParam("limit", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER_ERROR"));

        mockMvc.perform(get(path()).queryParam("limit", "11"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER_ERROR"));

        mockMvc.perform(get(path()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.regionName").value(REGION_NAME))
                .andExpect(jsonPath("$.data.items").isEmpty());
    }

    @Test
    void regionId가_법정동_코드가_아니면_공통_400을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/regions/invalid/reports/lowest-prices"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER_ERROR"));
    }

    @Test
    void 동네_최저가_조회_계약을_OpenAPI에_노출한다() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/regions/{regionId}/reports/lowest-prices'].get")
                        .exists())
                .andExpect(jsonPath("$.paths['/api/v1/regions/{regionId}/reports/lowest-prices'].get.responses['200']")
                        .exists())
                .andExpect(jsonPath("$.paths['/api/v1/regions/{regionId}/reports/lowest-prices'].get.responses['400']")
                        .exists());
    }

    private void ensureRegion() {
        final Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM regions WHERE region_id = ?", Integer.class, REGION_ID);
        if (count == null || count == 0) {
            jdbcTemplate.update(
                    "INSERT INTO regions (region_id, region_name) VALUES (?, ?)", REGION_ID, REGION_NAME);
        }
    }

    private Item saveItem(final String name) {
        return itemJpaRepository.saveAndFlush(new Item(
                name + "-" + UUID.randomUUID(), "1kg", null, ItemCategory.ROOT_VEGETABLES));
    }

    private Store saveStore(final String name) {
        return storeJpaRepository.saveAndFlush(new Store(
                "kakao-" + UUID.randomUUID().toString().substring(0, 8), name, null, null, "테스트 주소", null,
                null, null, null, new BigDecimal("127.0000000000"),
                new BigDecimal("37.5000000000"), null));
    }

    private void savePublicPrice(final Item item, final LocalDate date, final int price) {
        publicPriceJpaRepository.saveAndFlush(new PublicPrice(item.id(), REGION_ID, price, date));
    }

    private UserReport saveReport(
            final Item item,
            final Store store,
            final LocalDate date,
            final int price,
            final Integer publicPriceDiff,
            final String priceDiffRate) {
        return saveReport(item, store, date, price, publicPriceDiff, priceDiffRate, item.defaultUnit());
    }

    private UserReport saveReport(
            final Item item,
            final Store store,
            final LocalDate date,
            final int price,
            final Integer publicPriceDiff,
            final String priceDiffRate,
            final String unit) {
        final UserReport report = userReportJpaRepository.saveAndFlush(new UserReport(
                REGION_ID, ReportType.OBSERVED, store == null ? null : store.id(), item.id(), null,
                price, unit, BigDecimal.ONE, publicPriceDiff, new BigDecimal(priceDiffRate), null));
        jdbcTemplate.update(
                "UPDATE user_reports SET report_date = ? WHERE report_id = ?", date, report.id());
        return report;
    }

    private String path() {
        return "/api/v1/regions/" + REGION_ID + "/reports/lowest-prices";
    }
}
