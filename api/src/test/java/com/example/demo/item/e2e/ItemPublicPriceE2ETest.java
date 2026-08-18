package com.example.demo.item.e2e;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.item.domain.Item;
import com.example.demo.item.domain.ItemCategory;
import com.example.demo.item.domain.PublicPrice;
import com.example.demo.item.infrastructure.ItemJpaRepository;
import com.example.demo.item.infrastructure.PublicPriceJpaRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ItemPublicPriceE2ETest {

    private static final String REGION_ID = "1121510100";
    private static final String OTHER_REGION_ID = "1168010100";
    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

    private final MockMvc mockMvc;
    private final ItemJpaRepository itemJpaRepository;
    private final PublicPriceJpaRepository publicPriceJpaRepository;
    private final JdbcTemplate jdbcTemplate;
    private Long potatoId;
    private Long onionId;
    private LocalDate today;

    @Autowired
    ItemPublicPriceE2ETest(
            final MockMvc mockMvc,
            final ItemJpaRepository itemJpaRepository,
            final PublicPriceJpaRepository publicPriceJpaRepository,
            final JdbcTemplate jdbcTemplate) {
        this.mockMvc = mockMvc;
        this.itemJpaRepository = itemJpaRepository;
        this.publicPriceJpaRepository = publicPriceJpaRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM item_favorites");
        publicPriceJpaRepository.deleteAll();
        itemJpaRepository.deleteAll();
        today = LocalDate.now(SERVICE_ZONE);
        final Item potato = itemJpaRepository.save(
                new Item("감자", "1kg", null, ItemCategory.ROOT_VEGETABLES));
        final Item onion = itemJpaRepository.save(
                new Item("양파", null, null, ItemCategory.SEASONINGS));
        potatoId = potato.id();
        onionId = onion.id();
        publicPriceJpaRepository.save(new PublicPrice(potatoId, REGION_ID, 3000, today.minusDays(3)));
        publicPriceJpaRepository.save(new PublicPrice(potatoId, REGION_ID, 3500, today.minusDays(1)));
        publicPriceJpaRepository.save(new PublicPrice(potatoId, REGION_ID, 3800, today));
        publicPriceJpaRepository.save(new PublicPrice(potatoId, REGION_ID, 2000, today.minusMonths(2)));
        publicPriceJpaRepository.save(new PublicPrice(potatoId, REGION_ID, 1000, today.minusYears(2)));
        publicPriceJpaRepository.save(new PublicPrice(potatoId, OTHER_REGION_ID, 9999, today));
    }

    @Test
    @DisplayName("WEEK 기간은 최근 7일 시세만 날짜 오름차순으로 반환한다")
    void returnsWeeklyTrendInDateOrder() throws Exception {
        mockMvc.perform(get(path(potatoId))
                        .param("regionId", REGION_ID)
                        .param("period", "WEEK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemId").value(potatoId))
                .andExpect(jsonPath("$.defaultUnit").value("1kg"))
                .andExpect(jsonPath("$.period").value("WEEK"))
                .andExpect(jsonPath("$.points[*].price").value(contains(3000, 3500, 3800)))
                .andExpect(jsonPath("$.points[0].date").value(today.minusDays(3).toString()))
                .andExpect(jsonPath("$.points[2].date").value(today.toString()));
    }

    @Test
    @DisplayName("MONTH 기간은 최근 1개월, YEAR 기간은 최근 1년 시세를 선택한다")
    void selectsRangeByPeriod() throws Exception {
        mockMvc.perform(get(path(potatoId)).param("regionId", REGION_ID).param("period", "MONTH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.points[*].price").value(contains(3000, 3500, 3800)));

        mockMvc.perform(get(path(potatoId)).param("regionId", REGION_ID).param("period", "YEAR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.points[*].price").value(contains(2000, 3000, 3500, 3800)));
    }

    @Test
    @DisplayName("period를 생략하면 MONTH로 조회한다")
    void defaultsToMonth() throws Exception {
        mockMvc.perform(get(path(potatoId)).param("regionId", REGION_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.period").value("MONTH"))
                .andExpect(jsonPath("$.points[*].price").value(contains(3000, 3500, 3800)));
    }

    @Test
    @DisplayName("같은 날짜에 여러 가격이 있으면 가장 최근에 저장된 가격만 남긴다")
    void keepsLatestPricePerDate() throws Exception {
        publicPriceJpaRepository.save(new PublicPrice(potatoId, REGION_ID, 4200, today));

        mockMvc.perform(get(path(potatoId)).param("regionId", REGION_ID).param("period", "WEEK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.points[*].price").value(contains(3000, 3500, 4200)));
    }

    @Test
    @DisplayName("기간 내 가격이 없으면 200과 빈 points를 반환한다")
    void returnsEmptyPointsWhenNoPriceInPeriod() throws Exception {
        mockMvc.perform(get(path(onionId)).param("regionId", REGION_ID).param("period", "WEEK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemId").value(onionId))
                .andExpect(jsonPath("$.defaultUnit").doesNotExist())
                .andExpect(jsonPath("$.points").isEmpty());
    }

    @Test
    @DisplayName("존재하지 않는 품목은 404를 반환한다")
    void returnsNotFoundForUnknownItem() throws Exception {
        mockMvc.perform(get(path(999_999L)).param("regionId", REGION_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("regionId 누락과 허용되지 않는 period는 400을 반환한다")
    void rejectsInvalidRequest() throws Exception {
        mockMvc.perform(get(path(potatoId)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(path(potatoId)).param("regionId", REGION_ID).param("period", "DECADE"))
                .andExpect(status().isBadRequest());
    }

    private String path(final Long itemId) {
        return "/api/v1/items/" + itemId + "/public-prices";
    }
}
