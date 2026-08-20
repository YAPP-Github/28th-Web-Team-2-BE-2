package com.example.demo.report.e2e;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.item.domain.Item;
import com.example.demo.item.domain.ItemCategory;
import com.example.demo.item.infrastructure.ItemJpaRepository;
import com.example.demo.report.domain.ReportType;
import com.example.demo.report.domain.UserReport;
import com.example.demo.report.infrastructure.UserReportJpaRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class RegionItemReportContractE2ETest {

    private static final String REGION_ID = "1121510100";
    private static final String UNKNOWN_REGION_ID = "1168010100";

    private final MockMvc mockMvc;
    private final ItemJpaRepository itemJpaRepository;
    private final UserReportJpaRepository userReportJpaRepository;
    private final JdbcTemplate jdbcTemplate;
    private Long itemId;

    @Autowired
    RegionItemReportContractE2ETest(
            final MockMvc mockMvc,
            final ItemJpaRepository itemJpaRepository,
            final UserReportJpaRepository userReportJpaRepository,
            final JdbcTemplate jdbcTemplate) {
        this.mockMvc = mockMvc;
        this.itemJpaRepository = itemJpaRepository;
        this.userReportJpaRepository = userReportJpaRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @BeforeEach
    void setUp() {
        userReportJpaRepository.deleteAll();
        itemJpaRepository.deleteAll();
        jdbcTemplate.update("DELETE FROM regions");
        jdbcTemplate.update(
                "INSERT INTO regions (region_id, region_name) VALUES (?, ?)",
                REGION_ID, "서울특별시 광진구 중곡동");
        final Item item = itemJpaRepository.save(
                new Item("감자", "1kg", null, ItemCategory.ROOT_VEGETABLES));
        itemId = item.id();
        userReportJpaRepository.save(new UserReport(
                REGION_ID,
                ReportType.PURCHASE,
                null,
                itemId,
                1L,
                2_500,
                "1kg",
                BigDecimal.ONE,
                -500,
                new BigDecimal("-16.67"),
                null));
    }

    @Test
    void returnsReportedAtForAValidRegionAndItem() throws Exception {
        mockMvc.perform(get(path(REGION_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reports[0].reportedAt").exists())
                .andExpect(jsonPath("$.data.reports[0].reportedDate").doesNotExist());
    }

    @Test
    void rejectsAnUnknownRegion() throws Exception {
        mockMvc.perform(get(path(UNKNOWN_REGION_ID))).andExpect(status().isNotFound());
    }

    private String path(final String regionId) {
        return "/api/v1/regions/" + regionId + "/items/" + itemId + "/reports";
    }
}
