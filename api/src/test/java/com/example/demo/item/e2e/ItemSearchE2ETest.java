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
class ItemSearchE2ETest {

    private static final String SEARCH_PATH = "/api/v1/products/search";
    private static final String REGION_ID = "1121510100";

    private final MockMvc mockMvc;
    private final ItemJpaRepository itemJpaRepository;
    private final PublicPriceJpaRepository publicPriceJpaRepository;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    ItemSearchE2ETest(
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
        final LocalDate today = LocalDate.now();
        final Item redPepper = itemJpaRepository.save(
                new Item("붉은고추", "100g", null, ItemCategory.SEASONINGS));
        itemJpaRepository.save(new Item("청양고추", "1kg", null, ItemCategory.PEPPERS));
        itemJpaRepository.save(new Item("꽈리고추", "1kg", null, ItemCategory.PEPPERS));
        itemJpaRepository.save(new Item("감자", "1kg", null, ItemCategory.ROOT_VEGETABLES));
        publicPriceJpaRepository.save(
                new PublicPrice(redPepper.id(), REGION_ID, 4000, today.minusDays(1)));
        publicPriceJpaRepository.save(new PublicPrice(redPepper.id(), REGION_ID, 5000, today));
    }

    @Test
    @DisplayName("품목명 부분 일치 검색은 인증 없이 가격 요약과 함께 조회된다")
    void searchesItemsByPartialName() throws Exception {
        mockMvc.perform(get(SEARCH_PATH)
                        .param("keyword", "고추")
                        .param("regionId", REGION_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(3))
                .andExpect(jsonPath("$.data.items[*].itemName")
                        .value(contains("꽈리고추", "붉은고추", "청양고추")))
                .andExpect(jsonPath("$.data.items[1].price").value(5000))
                .andExpect(jsonPath("$.data.items[1].priceDiffRate").value(25.0))
                .andExpect(jsonPath("$.data.pagination.limit").value(30))
                .andExpect(jsonPath("$.data.pagination.offset").value(0))
                .andExpect(jsonPath("$.data.pagination.hasNext").value(false));
    }

    @Test
    @DisplayName("공백 검색어는 400으로 거절된다")
    void rejectsBlankKeyword() throws Exception {
        mockMvc.perform(get(SEARCH_PATH).param("keyword", "   ").param("regionId", REGION_ID))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("일치하는 품목이 없으면 빈 목록을 반환한다")
    void returnsEmptyListWhenNoMatch() throws Exception {
        mockMvc.perform(get(SEARCH_PATH).param("keyword", "없는품목").param("regionId", REGION_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(0))
                .andExpect(jsonPath("$.data.items").isEmpty())
                .andExpect(jsonPath("$.data.pagination.hasNext").value(false));
    }

    @Test
    @DisplayName("offset 경계에서 hasNext와 목록이 정확하다")
    void paginatesByOffset() throws Exception {
        mockMvc.perform(get(SEARCH_PATH)
                        .param("keyword", "고추")
                        .param("regionId", REGION_ID)
                        .param("limit", "2")
                        .param("offset", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[*].itemName").value(contains("꽈리고추", "붉은고추")))
                .andExpect(jsonPath("$.data.pagination.hasNext").value(true));

        mockMvc.perform(get(SEARCH_PATH)
                        .param("keyword", "고추")
                        .param("regionId", REGION_ID)
                        .param("limit", "2")
                        .param("offset", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[*].itemName").value(contains("붉은고추", "청양고추")))
                .andExpect(jsonPath("$.data.pagination.offset").value(1))
                .andExpect(jsonPath("$.data.pagination.hasNext").value(false));
    }

    @Test
    @DisplayName("limit과 offset의 허용 범위를 벗어나면 400으로 거절된다")
    void rejectsOutOfRangePagination() throws Exception {
        mockMvc.perform(get(SEARCH_PATH)
                        .param("keyword", "고추")
                        .param("regionId", REGION_ID)
                        .param("offset", "-1"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(SEARCH_PATH)
                        .param("keyword", "고추")
                        .param("regionId", REGION_ID)
                        .param("limit", "0"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(SEARCH_PATH)
                        .param("keyword", "고추")
                        .param("regionId", REGION_ID)
                        .param("limit", "101"))
                .andExpect(status().isBadRequest());
    }
}
