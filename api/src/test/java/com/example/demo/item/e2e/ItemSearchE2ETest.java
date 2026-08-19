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
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ItemSearchE2ETest {

    private static final String SEARCH_PATH = "/api/v1/items/search";
    private static final String REGION_ID = "1121510100";

    private final MockMvc mockMvc;
    private final ItemJpaRepository itemJpaRepository;
    private final PublicPriceJpaRepository publicPriceJpaRepository;
    private final JdbcTemplate jdbcTemplate;
    private final String accessSecret;

    @Autowired
    ItemSearchE2ETest(
            final MockMvc mockMvc,
            final ItemJpaRepository itemJpaRepository,
            final PublicPriceJpaRepository publicPriceJpaRepository,
            final JdbcTemplate jdbcTemplate,
            @Value("${jwt.access-secret}") final String accessSecret) {
        this.mockMvc = mockMvc;
        this.itemJpaRepository = itemJpaRepository;
        this.publicPriceJpaRepository = publicPriceJpaRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.accessSecret = accessSecret;
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
                .andExpect(jsonPath("$.data.totalCount").value(3))
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
    @DisplayName("offset이 있고 limit이 전체 건수보다 크면 totalCount가 전체 건수 그대로다")
    void keepsTotalCountWhenLimitExceedsTotal() throws Exception {
        mockMvc.perform(get(SEARCH_PATH)
                        .param("keyword", "고추")
                        .param("regionId", REGION_ID)
                        .param("offset", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(3))
                .andExpect(jsonPath("$.data.items[*].itemName").value(contains("붉은고추", "청양고추")))
                .andExpect(jsonPath("$.data.pagination.hasNext").value(false));
    }

    @Test
    @DisplayName("offset이 전체 건수를 넘으면 빈 목록이지만 totalCount는 유지된다")
    void keepsTotalCountWhenOffsetPastEnd() throws Exception {
        mockMvc.perform(get(SEARCH_PATH)
                        .param("keyword", "고추")
                        .param("regionId", REGION_ID)
                        .param("offset", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(3))
                .andExpect(jsonPath("$.data.items").isEmpty())
                .andExpect(jsonPath("$.data.pagination.hasNext").value(false));
    }

    @Test
    @DisplayName("게스트 토큰으로도 검색할 수 있다")
    void allowsGuestToken() throws Exception {
        mockMvc.perform(get(SEARCH_PATH)
                        .param("keyword", "고추")
                        .param("regionId", REGION_ID)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + guestAccessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(3));
    }

    @Test
    @DisplayName("기준일 가격이 없는 품목은 price와 priceDiffRate가 null이다")
    void returnsNullPriceWhenNoPublicPrice() throws Exception {
        mockMvc.perform(get(SEARCH_PATH).param("keyword", "청양").param("regionId", REGION_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].price").value(nullValue()))
                .andExpect(jsonPath("$.data.items[0].priceDiffRate").value(nullValue()));
    }

    @Test
    @DisplayName("검색 API가 OpenAPI 문서에 노출된다")
    void exposesApiDocs() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/items/search'].get").exists())
                .andExpect(jsonPath(
                                "$.paths['/api/v1/items/search'].get.parameters[?(@.name == 'keyword')]")
                        .isNotEmpty())
                .andExpect(jsonPath(
                                "$.paths['/api/v1/items/search'].get.parameters[?(@.name == 'offset')]")
                        .isNotEmpty());
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
