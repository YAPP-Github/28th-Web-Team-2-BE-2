package com.example.demo.item.e2e;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.item.domain.Item;
import com.example.demo.item.domain.ItemCategory;
import com.example.demo.item.domain.PublicPrice;
import com.example.demo.item.infrastructure.ItemJpaRepository;
import com.example.demo.item.infrastructure.PublicPriceJpaRepository;
import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

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
        // 기간별로 결과가 갈리도록 배치한다. WEEK 구간은 (today-7, today] 이므로
        // today-7 은 제외되고 today-6 은 포함되어야 한다.
        savePrice(2500, today.minusDays(7));
        savePrice(2700, today.minusDays(6));
        savePrice(3000, today.minusDays(3));
        savePrice(3500, today.minusDays(1));
        savePrice(3800, today);
        savePrice(2200, today.minusDays(10));
        savePrice(2100, today.minusMonths(1));
        savePrice(2000, today.minusMonths(2));
        savePrice(1000, today.minusYears(2));
        publicPriceJpaRepository.save(new PublicPrice(potatoId, OTHER_REGION_ID, 9999, today));
    }

    private void savePrice(final int price, final java.time.LocalDate priceDate) {
        publicPriceJpaRepository.save(new PublicPrice(potatoId, REGION_ID, price, priceDate));
    }

    @Test
    @DisplayName("WEEK 기간은 구간 시작일을 제외하고 날짜 오름차순으로 반환한다")
    void returnsWeeklyTrendInDateOrder() throws Exception {
        final MvcResult result = mockMvc.perform(get(path(potatoId))
                        .param("regionId", REGION_ID)
                        .param("period", "WEEK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemId").value(potatoId))
                .andExpect(jsonPath("$.defaultUnit").value("1kg"))
                .andExpect(jsonPath("$.period").value("WEEK"))
                .andExpect(jsonPath("$.points[0].date").value(today.minusDays(6).toString()))
                .andExpect(jsonPath("$.points[3].date").value(today.toString()))
                .andReturn();

        Assertions.assertThat(JsonPath.<List<Integer>>read(
                        result.getResponse().getContentAsString(), "$.points[*].price"))
                .containsExactly(2700, 3000, 3500, 3800);
        Assertions.assertThat(JsonPath.<List<String>>read(
                        result.getResponse().getContentAsString(), "$.points[*].date"))
                .containsExactly(
                        today.minusDays(6).toString(),
                        today.minusDays(3).toString(),
                        today.minusDays(1).toString(),
                        today.toString());
    }

    @Test
    @DisplayName("기간마다 선택되는 구간이 다르다")
    void selectsRangeByPeriod() throws Exception {
        final MvcResult monthResult = mockMvc.perform(
                        get(path(potatoId)).param("regionId", REGION_ID).param("period", "MONTH"))
                .andExpect(status().isOk())
                .andReturn();

        Assertions.assertThat(JsonPath.<List<Integer>>read(
                        monthResult.getResponse().getContentAsString(), "$.points[*].price"))
                .containsExactly(2200, 2500, 2700, 3000, 3500, 3800);
        Assertions.assertThat(JsonPath.<List<String>>read(
                        monthResult.getResponse().getContentAsString(), "$.points[*].date"))
                .containsExactly(
                        today.minusDays(10).toString(),
                        today.minusDays(7).toString(),
                        today.minusDays(6).toString(),
                        today.minusDays(3).toString(),
                        today.minusDays(1).toString(),
                        today.toString());

        final MvcResult yearResult = mockMvc.perform(
                        get(path(potatoId)).param("regionId", REGION_ID).param("period", "YEAR"))
                .andExpect(status().isOk())
                .andReturn();

        Assertions.assertThat(JsonPath.<List<Integer>>read(
                        yearResult.getResponse().getContentAsString(), "$.points[*].price"))
                .containsExactly(2000, 2100, 2200, 2500, 2700, 3000, 3500, 3800);
        Assertions.assertThat(JsonPath.<List<String>>read(
                        yearResult.getResponse().getContentAsString(), "$.points[*].date"))
                .containsExactly(
                        today.minusMonths(2).toString(),
                        today.minusMonths(1).toString(),
                        today.minusDays(10).toString(),
                        today.minusDays(7).toString(),
                        today.minusDays(6).toString(),
                        today.minusDays(3).toString(),
                        today.minusDays(1).toString(),
                        today.toString());
    }

    @Test
    @DisplayName("period를 생략하면 MONTH로 조회한다")
    void defaultsToMonth() throws Exception {
        final MvcResult result = mockMvc.perform(get(path(potatoId)).param("regionId", REGION_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.period").value("MONTH"))
                .andReturn();

        Assertions.assertThat(JsonPath.<List<Integer>>read(
                        result.getResponse().getContentAsString(), "$.points[*].price"))
                .containsExactly(2200, 2500, 2700, 3000, 3500, 3800);
        Assertions.assertThat(JsonPath.<List<String>>read(
                        result.getResponse().getContentAsString(), "$.points[*].date"))
                .containsExactly(
                        today.minusDays(10).toString(),
                        today.minusDays(7).toString(),
                        today.minusDays(6).toString(),
                        today.minusDays(3).toString(),
                        today.minusDays(1).toString(),
                        today.toString());
    }

    @Test
    @DisplayName("기준일은 지역의 최신 시세일이라 수집이 밀려도 구간이 비지 않는다")
    void anchorsWindowOnLatestPriceDate() throws Exception {
        publicPriceJpaRepository.deleteAll();
        savePrice(1500, today.minusDays(40));
        savePrice(1600, today.minusDays(38));

        final MvcResult result = mockMvc.perform(
                        get(path(potatoId)).param("regionId", REGION_ID).param("period", "WEEK"))
                .andExpect(status().isOk())
                .andReturn();

        Assertions.assertThat(JsonPath.<List<Integer>>read(
                        result.getResponse().getContentAsString(), "$.points[*].price"))
                .containsExactly(1500, 1600);
        Assertions.assertThat(JsonPath.<List<String>>read(
                        result.getResponse().getContentAsString(), "$.points[*].date"))
                .containsExactly(today.minusDays(40).toString(), today.minusDays(38).toString());
    }

    @Test
    @DisplayName("같은 날짜에 여러 가격이 있으면 가장 최근에 저장된 가격만 남긴다")
    void keepsLatestPricePerDate() throws Exception {
        savePrice(4200, today);

        mockMvc.perform(get(path(potatoId)).param("regionId", REGION_ID).param("period", "WEEK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.points[*].price").value(contains(2700, 3000, 3500, 4200)));
    }

    @Test
    @DisplayName("기간 내 가격이 없으면 200과 빈 points를 반환한다")
    void returnsEmptyPointsWhenNoPriceInPeriod() throws Exception {
        publicPriceJpaRepository.save(new PublicPrice(onionId, REGION_ID, 1200, today.minusDays(8)));

        mockMvc.perform(get(path(onionId)).param("regionId", REGION_ID).param("period", "WEEK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemId").value(onionId))
                .andExpect(jsonPath("$.defaultUnit").value(nullValue()))
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

    @Test
    @DisplayName("공공가격 추이 API가 OpenAPI 문서에 노출된다")
    void exposesApiDocs() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/items/{itemId}/public-prices'].get").exists())
                .andExpect(jsonPath(
                                "$.paths['/api/v1/items/{itemId}/public-prices'].get.parameters[?(@.name == 'period')]")
                        .isNotEmpty());
    }

    private String path(final Long itemId) {
        return "/api/v1/items/" + itemId + "/public-prices";
    }
}
