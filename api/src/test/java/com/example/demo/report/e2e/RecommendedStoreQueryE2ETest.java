package com.example.demo.report.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.item.domain.Item;
import com.example.demo.item.domain.ItemCategory;
import com.example.demo.item.infrastructure.ItemJpaRepository;
import com.example.demo.report.domain.ReportType;
import com.example.demo.report.domain.Store;
import com.example.demo.report.domain.UserReport;
import com.example.demo.report.infrastructure.UserReportJpaRepository;
import com.example.demo.store.infrastructure.persistence.StoreJpaRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class RecommendedStoreQueryE2ETest {

    private static final String REGION_ID = "1121510100";

    private final MockMvc mockMvc;
    private final UserReportJpaRepository userReportJpaRepository;
    private final StoreJpaRepository storeJpaRepository;
    private final ItemJpaRepository itemJpaRepository;

    @Autowired
    RecommendedStoreQueryE2ETest(
            final MockMvc mockMvc,
            final UserReportJpaRepository userReportJpaRepository,
            final StoreJpaRepository storeJpaRepository,
            final ItemJpaRepository itemJpaRepository) {
        this.mockMvc = mockMvc;
        this.userReportJpaRepository = userReportJpaRepository;
        this.storeJpaRepository = storeJpaRepository;
        this.itemJpaRepository = itemJpaRepository;
    }

    @BeforeEach
    void setUp() {
        userReportJpaRepository.deleteAll();
        storeJpaRepository.deleteAll();
        itemJpaRepository.deleteAll();
    }

    @Test
    void 같은_가게의_같은_품목에_저가와_고가_제보가_있으면_저가_제보를_추천한다() throws Exception {
        final Store store = storeJpaRepository.save(new Store(
                "recommendation-store",
                "추천 마트",
                null,
                null,
                "서울 강남구",
                null,
                null,
                null,
                null,
                new BigDecimal("127.0632"),
                new BigDecimal("37.5088"),
                null));
        final Item item = itemJpaRepository.save(
                new Item("양파", "1kg", null, ItemCategory.SEASONINGS));

        userReportJpaRepository.save(new UserReport(
                REGION_ID,
                ReportType.PURCHASE,
                store.id(),
                item.id(),
                1L,
                2500,
                "1kg",
                BigDecimal.ONE,
                -500,
                new BigDecimal("-16.67"),
                null));
        userReportJpaRepository.save(new UserReport(
                REGION_ID,
                ReportType.PURCHASE,
                store.id(),
                item.id(),
                2L,
                3500,
                "1kg",
                BigDecimal.ONE,
                500,
                new BigDecimal("16.67"),
                null));

        assertThat(userReportJpaRepository.findLatestCheapReports(REGION_ID))
                .extracting(UserReport::price)
                .containsExactly(2500);

        mockMvc.perform(get("/api/v1/stores/recommendation")
                        .param("regionId", REGION_ID)
                        .param("latitude", "37.5088")
                        .param("longitude", "127.0632")
                        .param("radius", "2000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(1))
                .andExpect(jsonPath("$.data.stores[0].storeId").value(store.id()))
                .andExpect(jsonPath("$.data.stores[0].cheapItemCount").value(1))
                .andExpect(jsonPath("$.data.stores[0].cheapItems[0]").value("양파"));
    }
}
