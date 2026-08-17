package com.example.demo.item.e2e;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.example.demo.item.domain.PublicPrice;
import com.example.demo.item.infrastructure.ItemJpaRepository;
import com.example.demo.item.infrastructure.PublicPriceJpaRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.persistence.EntityManagerFactory;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureMockMvc
class ItemQueryE2ETest {

    private static final String REGION_ID = "1121510100";
    private static final String OTHER_REGION_ID = "1168010100";
    private static final String SAME_DATE_PRICE_REGION_ID = "9999999999";

    private final MockMvc mockMvc;
    private final ItemJpaRepository itemJpaRepository;
    private final PublicPriceJpaRepository publicPriceJpaRepository;
    private final UserJpaRepository userJpaRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final JdbcTemplate jdbcTemplate;
    private final Statistics statistics;
    private final String accessSecret;
    private Long firstPotatoId;
    private Long secondPotatoId;
    private Long onionId;
    private LocalDate referenceDate;

    @Autowired
    ItemQueryE2ETest(
            final MockMvc mockMvc,
            final ItemJpaRepository itemJpaRepository,
            final PublicPriceJpaRepository publicPriceJpaRepository,
            final UserJpaRepository userJpaRepository,
            final JwtTokenProvider jwtTokenProvider,
            final JdbcTemplate jdbcTemplate,
            final EntityManagerFactory entityManagerFactory,
            @Value("${jwt.access-secret}") final String accessSecret) {
        this.mockMvc = mockMvc;
        this.itemJpaRepository = itemJpaRepository;
        this.publicPriceJpaRepository = publicPriceJpaRepository;
        this.userJpaRepository = userJpaRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.jdbcTemplate = jdbcTemplate;
        this.statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        this.accessSecret = accessSecret;
    }

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM item_favorites");
        publicPriceJpaRepository.deleteAll();
        itemJpaRepository.deleteAll();
        userJpaRepository.deleteAll();
        referenceDate = LocalDate.now();

        final Item potato = itemJpaRepository.save(new Item("감자", "1kg"));
        final Item onion = itemJpaRepository.save(new Item("양파", "1kg"));
        final Item greenOnion = itemJpaRepository.save(new Item("대파", "1단"));
        final Item carrot = itemJpaRepository.save(new Item("당근", "1kg"));
        itemJpaRepository.save(new Item("양배추", null));
        final Item secondPotato = itemJpaRepository.save(new Item("감자", "1kg"));
        firstPotatoId = potato.id();
        secondPotatoId = secondPotato.id();
        onionId = onion.id();
        publicPriceJpaRepository.save(
                new PublicPrice(potato.id(), REGION_ID, 3000, referenceDate.minusDays(2)));
        publicPriceJpaRepository.save(new PublicPrice(potato.id(), REGION_ID, 3500, referenceDate));
        publicPriceJpaRepository.save(
                new PublicPrice(onion.id(), REGION_ID, 3000, referenceDate.minusDays(3)));
        publicPriceJpaRepository.save(new PublicPrice(onion.id(), REGION_ID, 2800, referenceDate));
        publicPriceJpaRepository.save(
                new PublicPrice(greenOnion.id(), REGION_ID, 2100, referenceDate.minusDays(4)));
        publicPriceJpaRepository.save(new PublicPrice(greenOnion.id(), REGION_ID, 2100, referenceDate));
        publicPriceJpaRepository.save(new PublicPrice(carrot.id(), REGION_ID, 4000, referenceDate));
        publicPriceJpaRepository.save(new PublicPrice(secondPotato.id(), REGION_ID, 3500, referenceDate));
        publicPriceJpaRepository.save(new PublicPrice(potato.id(), OTHER_REGION_ID, 9999, referenceDate));
    }

    @Test
    void 비로그인과_지원하지_않는_GUEST_토큰은_모든_isLiked를_false로_조회한다() throws Exception {
        final User user = saveUser("GUEST subject 사용자");
        addFavorite(user.id(), firstPotatoId);

        mockMvc.perform(itemListRequest())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].isLiked")
                        .value(contains(false, false, false, false, false, false)));

        mockMvc.perform(itemListRequest()
                        .header(HttpHeaders.AUTHORIZATION, bearer(guestAccessToken(user.id()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].isLiked")
                        .value(contains(false, false, false, false, false, false)));
    }

    @Test
    void ROLE_USER는_자신의_찜만_isLiked로_조회한다() throws Exception {
        final User currentUser = saveUser("현재 사용자");
        final User otherUser = saveUser("다른 사용자");
        addFavorite(currentUser.id(), firstPotatoId);
        addFavorite(otherUser.id(), onionId);

        mockMvc.perform(itemListRequest()
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken(currentUser))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].itemId").value(firstPotatoId))
                .andExpect(jsonPath("$.items[0].isLiked").value(true))
                .andExpect(jsonPath("$.items[5].itemId").value(onionId))
                .andExpect(jsonPath("$.items[5].isLiked").value(false));

        mockMvc.perform(itemListRequest()
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken(otherUser))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].isLiked").value(false))
                .andExpect(jsonPath("$.items[5].isLiked").value(true));
    }

    @Test
    void ROLE_USER의_현재_페이지_찜은_한_번의_일괄_쿼리로_조회한다() throws Exception {
        final User user = saveUser("일괄 조회 사용자");
        itemJpaRepository.findAll().forEach(item -> addFavorite(user.id(), item.id()));
        statistics.clear();

        mockMvc.perform(itemListRequest()
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken(user))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].isLiked")
                        .value(contains(true, true, true, true, true, true)));

        assertThat(statistics.getPrepareStatementCount()).isEqualTo(5);
    }

    @Test
    void favoriteOnly는_현재_ROLE_USER의_찜만_isLiked_true로_조회한다() throws Exception {
        final User currentUser = saveUser("현재 찜 필터 사용자");
        final User otherUser = saveUser("다른 찜 필터 사용자");
        addFavorite(currentUser.id(), firstPotatoId);
        addFavorite(currentUser.id(), secondPotatoId);
        addFavorite(otherUser.id(), onionId);

        mockMvc.perform(itemListRequest()
                        .queryParam("favoriteOnly", "true")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken(currentUser))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(2))
                .andExpect(jsonPath("$.items[*].itemId")
                        .value(contains(firstPotatoId.intValue(), secondPotatoId.intValue())))
                .andExpect(jsonPath("$.items[*].isLiked").value(contains(true, true)))
                .andExpect(jsonPath("$.hasNext").value(false));
    }

    @Test
    void favoriteOnly는_비로그인과_같은_subject의_ROLE_GUEST에_401을_응답한다() throws Exception {
        final User user = saveUser("찜 필터 GUEST subject 사용자");
        addFavorite(user.id(), firstPotatoId);

        mockMvc.perform(itemListRequest().queryParam("favoriteOnly", "true"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(itemListRequest()
                        .queryParam("favoriteOnly", "true")
                        .header(HttpHeaders.AUTHORIZATION, bearer(guestAccessToken(user.id()))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void favoriteOnly를_생략하거나_false로_요청하면_기존_공개_조회가_유지된다() throws Exception {
        final User user = saveUser("공개 조회 사용자");
        addFavorite(user.id(), firstPotatoId);

        mockMvc.perform(itemListRequest())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(6))
                .andExpect(jsonPath("$.items[*].isLiked")
                        .value(contains(false, false, false, false, false, false)));

        mockMvc.perform(itemListRequest().queryParam("favoriteOnly", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(6))
                .andExpect(jsonPath("$.items[*].isLiked")
                        .value(contains(false, false, false, false, false, false)));
    }

    @Test
    void favoriteOnly는_검색_가격정렬_페이지네이션과_빈_결과의_기준이_된다() throws Exception {
        final User user = saveUser("찜 필터 조합 사용자");
        final User otherUser = saveUser("찜 필터 조합 다른 사용자");
        final Item otherUsersPotato = itemJpaRepository.save(new Item("감자", "1kg"));
        publicPriceJpaRepository.save(
                new PublicPrice(otherUsersPotato.id(), REGION_ID, 3500, referenceDate));
        addFavorite(user.id(), firstPotatoId);
        addFavorite(user.id(), secondPotatoId);
        addFavorite(otherUser.id(), otherUsersPotato.id());

        mockMvc.perform(get("/api/v1/items")
                        .queryParam("regionId", REGION_ID)
                        .queryParam("favoriteOnly", "true")
                        .queryParam("keyword", "감자")
                        .queryParam("sort", "PRICE_DESC")
                        .queryParam("page", "0")
                        .queryParam("size", "1")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken(user))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(2))
                .andExpect(jsonPath("$.items[0].itemId").value(firstPotatoId.intValue()))
                .andExpect(jsonPath("$.items[0].isLiked").value(true))
                .andExpect(jsonPath("$.hasNext").value(true));

        mockMvc.perform(get("/api/v1/items")
                        .queryParam("regionId", REGION_ID)
                        .queryParam("favoriteOnly", "true")
                        .queryParam("keyword", "감자")
                        .queryParam("sort", "PRICE_DESC")
                        .queryParam("page", "1")
                        .queryParam("size", "1")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken(user))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(2))
                .andExpect(jsonPath("$.items[0].itemId").value(secondPotatoId.intValue()))
                .andExpect(jsonPath("$.items[0].isLiked").value(true))
                .andExpect(jsonPath("$.hasNext").value(false));

        mockMvc.perform(get("/api/v1/items")
                        .queryParam("regionId", REGION_ID)
                        .queryParam("favoriteOnly", "true")
                        .queryParam("keyword", "없는품목")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken(user))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(0))
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.hasNext").value(false));
    }

    @Test
    void 공개_품목과_공공가격_목록을_직접_성공_응답과_가격_변동으로_조회한다() throws Exception {
        mockMvc.perform(get("/api/v1/items")
                        .queryParam("regionId", REGION_ID)
                        .queryParam("sort", "NAME_ASC")
                        .queryParam("page", "0")
                        .queryParam("size", "6"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").doesNotExist())
                .andExpect(jsonPath("$.message").doesNotExist())
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.baseDate").value(referenceDate.toString()))
                .andExpect(jsonPath("$.totalCount").value(6))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[*].itemName")
                        .value(contains("감자", "감자", "당근", "대파", "양배추", "양파")))
                .andExpect(jsonPath("$.items[0].price").value(3500))
                .andExpect(jsonPath("$.items[0].priceGap").value(500))
                .andExpect(jsonPath("$.items[0].defaultUnit").value("1kg"))
                .andExpect(jsonPath("$.items[0].priceDiffRate").value(16.7))
                .andExpect(jsonPath("$.items[1].price").value(3500))
                .andExpect(jsonPath("$.items[1].priceGap").value(nullValue()))
                .andExpect(jsonPath("$.items[1].priceDiffRate").value(nullValue()))
                .andExpect(jsonPath("$.items[2].price").value(4000))
                .andExpect(jsonPath("$.items[2].priceGap").value(nullValue()))
                .andExpect(jsonPath("$.items[2].priceDiffRate").value(nullValue()))
                .andExpect(jsonPath("$.items[3].price").value(2100))
                .andExpect(jsonPath("$.items[3].priceGap").value(0))
                .andExpect(jsonPath("$.items[3].priceDiffRate").value(0.0))
                .andExpect(jsonPath("$.items[4].price").value(nullValue()))
                .andExpect(jsonPath("$.items[4].priceGap").value(nullValue()))
                .andExpect(jsonPath("$.items[4].defaultUnit").value(nullValue()))
                .andExpect(jsonPath("$.items[4].priceDiffRate").value(nullValue()))
                .andExpect(jsonPath("$.items[5].price").value(2800))
                .andExpect(jsonPath("$.items[5].priceGap").value(-200))
                .andExpect(jsonPath("$.items[5].priceDiffRate").value(-6.7))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(6))
                .andExpect(jsonPath("$.hasNext").isBoolean());
    }

    @Test
    void sort를_생략하면_NAME_ASC로_품목명과_품목_ID_순으로_정렬한다() throws Exception {
        mockMvc.perform(get("/api/v1/items")
                        .queryParam("regionId", REGION_ID)
                        .queryParam("page", "0")
                        .queryParam("size", "6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].itemName")
                        .value(contains("감자", "감자", "당근", "대파", "양배추", "양파")))
                .andExpect(jsonPath("$.items[0].itemId").value(firstPotatoId))
                .andExpect(jsonPath("$.items[1].itemId").value(secondPotatoId));
    }

    @Test
    void NAME_ASC는_DB_정렬_후_페이지와_hasNext를_응답한다() throws Exception {
        mockMvc.perform(get("/api/v1/items")
                        .queryParam("regionId", REGION_ID)
                        .queryParam("sort", "NAME_ASC")
                        .queryParam("page", "1")
                        .queryParam("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].itemName").value(contains("당근", "대파")))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.hasNext").value(true));

        mockMvc.perform(get("/api/v1/items")
                        .queryParam("regionId", REGION_ID)
                        .queryParam("sort", "NAME_ASC")
                        .queryParam("page", "2")
                        .queryParam("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].itemName").value(contains("양배추", "양파")))
                .andExpect(jsonPath("$.hasNext").value(false));
    }

    @Test
    void PRICE_ASC와_PRICE_DESC는_현재_지역의_최신_공공가격으로_정렬하고_null을_마지막에_둔다() throws Exception {
        mockMvc.perform(get("/api/v1/items")
                        .queryParam("regionId", REGION_ID)
                        .queryParam("sort", "PRICE_ASC")
                        .queryParam("page", "0")
                        .queryParam("size", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].itemName").value(contains("대파", "양파", "감자")))
                .andExpect(jsonPath("$.items[2].itemId").value(firstPotatoId))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(3))
                .andExpect(jsonPath("$.hasNext").value(true));

        mockMvc.perform(get("/api/v1/items")
                        .queryParam("regionId", REGION_ID)
                        .queryParam("sort", "PRICE_ASC")
                        .queryParam("page", "1")
                        .queryParam("size", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].itemName").value(contains("감자", "당근", "양배추")))
                .andExpect(jsonPath("$.items[0].itemId").value(secondPotatoId))
                .andExpect(jsonPath("$.items[2].price").value(nullValue()))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(3))
                .andExpect(jsonPath("$.hasNext").value(false));

        mockMvc.perform(get("/api/v1/items")
                        .queryParam("regionId", REGION_ID)
                        .queryParam("sort", "PRICE_DESC")
                        .queryParam("page", "0")
                        .queryParam("size", "6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].itemName")
                        .value(contains("당근", "감자", "감자", "양파", "대파", "양배추")))
                .andExpect(jsonPath("$.items[1].itemId").value(firstPotatoId))
                .andExpect(jsonPath("$.items[2].itemId").value(secondPotatoId))
                .andExpect(jsonPath("$.items[5].price").value(nullValue()));
    }

    @Test
    void 품목명을_trim한_부분_일치_결과에_세_정렬과_페이지네이션을_적용한다() throws Exception {
        mockMvc.perform(get("/api/v1/items")
                        .queryParam("regionId", REGION_ID)
                        .queryParam("keyword", "  파  ")
                        .queryParam("sort", "NAME_ASC")
                        .queryParam("page", "0")
                        .queryParam("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(2))
                .andExpect(jsonPath("$.items[*].itemName").value(contains("대파", "양파")))
                .andExpect(jsonPath("$.hasNext").value(false));

        mockMvc.perform(get("/api/v1/items")
                        .queryParam("regionId", REGION_ID)
                        .queryParam("keyword", "파")
                        .queryParam("sort", "PRICE_ASC")
                        .queryParam("page", "0")
                        .queryParam("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(2))
                .andExpect(jsonPath("$.items[*].itemName").value(contains("대파", "양파")));

        mockMvc.perform(get("/api/v1/items")
                        .queryParam("regionId", REGION_ID)
                        .queryParam("keyword", "파")
                        .queryParam("sort", "PRICE_DESC")
                        .queryParam("page", "0")
                        .queryParam("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(2))
                .andExpect(jsonPath("$.items[*].itemName").value(contains("양파")))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.hasNext").value(true));

        mockMvc.perform(get("/api/v1/items")
                        .queryParam("regionId", REGION_ID)
                        .queryParam("keyword", "파")
                        .queryParam("sort", "PRICE_DESC")
                        .queryParam("page", "1")
                        .queryParam("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(2))
                .andExpect(jsonPath("$.items[*].itemName").value(contains("대파")))
                .andExpect(jsonPath("$.hasNext").value(false));
    }

    @Test
    void 검색어가_빈_문자열이거나_공백이면_전체_목록을_조회하고_결과가_없으면_빈_목록을_응답한다()
            throws Exception {
        mockMvc.perform(get("/api/v1/items")
                        .queryParam("regionId", REGION_ID)
                        .queryParam("keyword", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(6));

        mockMvc.perform(get("/api/v1/items")
                        .queryParam("regionId", REGION_ID)
                        .queryParam("keyword", "   "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(6));

        mockMvc.perform(get("/api/v1/items")
                        .queryParam("regionId", REGION_ID)
                        .queryParam("keyword", "없는품목"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(0))
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.hasNext").value(false));
    }

    @Test
    void 지역_전체_최신일과_품목별_최신일이_달라도_품목별_가격과_priceGap으로_정렬한다()
            throws Exception {
        final LocalDate today = referenceDate;
        final Item itemWithOlderLatestPrice = itemJpaRepository.save(new Item("시금치", "1단"));
        final Item itemWithRegionLatestPrice = itemJpaRepository.save(new Item("토마토", "1kg"));
        final Item apple = itemJpaRepository.save(new Item("사과", "1kg"));
        final Item cucumber = itemJpaRepository.save(new Item("오이", "1개"));

        publicPriceJpaRepository.save(new PublicPrice(
                itemWithOlderLatestPrice.id(), REGION_ID, 1500, today.minusDays(2)));
        publicPriceJpaRepository.save(new PublicPrice(
                itemWithOlderLatestPrice.id(), REGION_ID, 1700, today.minusDays(1)));
        publicPriceJpaRepository.save(new PublicPrice(
                itemWithRegionLatestPrice.id(), REGION_ID, 4500, today.minusDays(1)));
        publicPriceJpaRepository.save(new PublicPrice(
                itemWithRegionLatestPrice.id(), REGION_ID, 5000, today));
        publicPriceJpaRepository.save(new PublicPrice(apple.id(), REGION_ID, 3000, today));
        publicPriceJpaRepository.save(new PublicPrice(cucumber.id(), REGION_ID, 3000, today));
        publicPriceJpaRepository.save(new PublicPrice(
                itemWithOlderLatestPrice.id(), OTHER_REGION_ID, 999, today));

        mockMvc.perform(get("/api/v1/items")
                        .queryParam("regionId", REGION_ID)
                        .queryParam("sort", "PRICE_ASC")
                        .queryParam("page", "0")
                        .queryParam("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].itemName")
                        .value(contains(
                                "시금치", "대파", "양파", "사과", "오이",
                                "감자", "감자", "당근", "토마토", "양배추")))
                .andExpect(jsonPath("$.items[0].price").value(1700))
                .andExpect(jsonPath("$.items[0].priceGap").value(200))
                .andExpect(jsonPath("$.items[3].price").value(3000))
                .andExpect(jsonPath("$.items[4].price").value(3000))
                .andExpect(jsonPath("$.items[8].price").value(5000))
                .andExpect(jsonPath("$.items[8].priceGap").value(500))
                .andExpect(jsonPath("$.items[9].price").value(nullValue()));

        mockMvc.perform(get("/api/v1/items")
                        .queryParam("regionId", REGION_ID)
                        .queryParam("sort", "PRICE_DESC")
                        .queryParam("page", "0")
                        .queryParam("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].itemName")
                        .value(contains(
                                "토마토", "당근", "감자", "감자", "사과",
                                "오이", "양파", "대파", "시금치", "양배추")))
                .andExpect(jsonPath("$.items[0].price").value(5000))
                .andExpect(jsonPath("$.items[0].priceGap").value(500))
                .andExpect(jsonPath("$.items[4].price").value(3000))
                .andExpect(jsonPath("$.items[5].price").value(3000))
                .andExpect(jsonPath("$.items[8].price").value(1700))
                .andExpect(jsonPath("$.items[8].priceGap").value(200))
                .andExpect(jsonPath("$.items[9].price").value(nullValue()));
    }

    @Test
    void 같은_날짜의_공공가격은_가장_큰_ID를_현재가로_사용하고_직전_같은날짜_가격과_priceGap을_계산한다()
            throws Exception {
        final LocalDate today = referenceDate;
        final Item item = itemJpaRepository.save(new Item("동일날짜품목", "1개"));
        final Item anotherItem = itemJpaRepository.save(new Item("동일날짜보조품목", "1개"));
        publicPriceJpaRepository.save(new PublicPrice(
                item.id(), SAME_DATE_PRICE_REGION_ID, 1000, today));
        publicPriceJpaRepository.save(new PublicPrice(
                item.id(), SAME_DATE_PRICE_REGION_ID, 1200, today));
        publicPriceJpaRepository.save(new PublicPrice(
                item.id(), SAME_DATE_PRICE_REGION_ID, 900, today.minusDays(1)));
        publicPriceJpaRepository.save(new PublicPrice(
                anotherItem.id(), SAME_DATE_PRICE_REGION_ID, 1300, today));

        mockMvc.perform(get("/api/v1/items")
                        .queryParam("regionId", SAME_DATE_PRICE_REGION_ID)
                        .queryParam("sort", "PRICE_ASC")
                        .queryParam("page", "0")
                        .queryParam("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].itemName").value("동일날짜품목"))
                .andExpect(jsonPath("$.items[0].price").value(1200))
                .andExpect(jsonPath("$.items[0].priceGap").value(300))
                .andExpect(jsonPath("$.items[0].priceDiffRate").value(33.3))
                .andExpect(jsonPath("$.items[1].itemName").value("동일날짜보조품목"))
                .andExpect(jsonPath("$.items[1].price").value(1300));
    }

    @Test
    void 직전_공공가격이_0이면_변동률은_null이다() throws Exception {
        final Item item = itemJpaRepository.save(new Item("직전가격0품목", "1개"));
        publicPriceJpaRepository.save(
                new PublicPrice(item.id(), SAME_DATE_PRICE_REGION_ID, 0, referenceDate.minusDays(1)));
        publicPriceJpaRepository.save(
                new PublicPrice(item.id(), SAME_DATE_PRICE_REGION_ID, 1000, referenceDate));

        mockMvc.perform(get("/api/v1/items")
                        .queryParam("regionId", SAME_DATE_PRICE_REGION_ID)
                        .queryParam("sort", "PRICE_DESC")
                        .queryParam("page", "0")
                        .queryParam("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].price").value(1000))
                .andExpect(jsonPath("$.items[0].priceGap").value(1000))
                .andExpect(jsonPath("$.items[0].priceDiffRate").value(nullValue()));
    }

    @Test
    void 지원하지_않는_sort는_기존_400_오류_계약을_응답한다() throws Exception {
        mockMvc.perform(get("/api/v1/items")
                        .queryParam("regionId", REGION_ID)
                        .queryParam("sort", "PRICE_RANDOM"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER_ERROR"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void 페이지_크기를_생략하면_10개를_기본값으로_사용한다() throws Exception {
        mockMvc.perform(get("/api/v1/items")
                        .queryParam("regionId", REGION_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10));
    }

    @Test
    void 음수_페이지는_bad_request를_응답한다() throws Exception {
        mockMvc.perform(get("/api/v1/items")
                        .queryParam("regionId", REGION_ID)
                        .queryParam("page", "-1")
                        .queryParam("size", "2"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    void 크기가_0이면_bad_request를_응답한다() throws Exception {
        mockMvc.perform(get("/api/v1/items")
                        .queryParam("regionId", REGION_ID)
                        .queryParam("page", "0")
                        .queryParam("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    void 숫자가_아닌_페이지는_공통_bad_request를_응답한다() throws Exception {
        mockMvc.perform(get("/api/v1/items")
                        .queryParam("regionId", REGION_ID)
                        .queryParam("page", "not-a-number")
                        .queryParam("size", "2"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER_ERROR"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void 지역을_생략하면_bad_request를_응답한다() throws Exception {
        mockMvc.perform(get("/api/v1/items"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER_ERROR"))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void 빈_페이지는_성공과_빈_목록을_응답한다() throws Exception {
        mockMvc.perform(get("/api/v1/items")
                        .queryParam("regionId", REGION_ID)
                        .queryParam("page", "2")
                        .queryParam("size", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.hasNext").value(false));
    }

    @Test
    void 범위를_초과한_페이지는_성공과_빈_목록을_응답한다() throws Exception {
        mockMvc.perform(get("/api/v1/items")
                        .queryParam("regionId", REGION_ID)
                        .queryParam("page", "10")
                        .queryParam("size", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.hasNext").value(false));
    }

    @Test
    void 최대_페이지_크기를_초과하면_bad_request를_응답한다() throws Exception {
        mockMvc.perform(get("/api/v1/items")
                        .queryParam("regionId", REGION_ID)
                        .queryParam("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER_ERROR"))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void 품목_조회_경로와_조회_파라미터를_OpenAPI에_노출한다() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/items'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/items'].get.parameters[?(@.name == 'regionId')]")
                        .isNotEmpty())
                .andExpect(jsonPath("$.paths['/api/v1/items'].get.parameters[?(@.name == 'page')]")
                        .isNotEmpty())
                .andExpect(jsonPath("$.paths['/api/v1/items'].get.parameters[?(@.name == 'size')]")
                        .isNotEmpty())
                .andExpect(jsonPath("$.paths['/api/v1/items'].get.parameters[?(@.name == 'sort')]")
                        .isNotEmpty())
                .andExpect(jsonPath("$.paths['/api/v1/items'].get.parameters[?(@.name == 'keyword')]")
                        .isNotEmpty())
                .andExpect(jsonPath("$.paths['/api/v1/items'].get.parameters[?(@.name == 'favoriteOnly')]")
                        .isNotEmpty())
                .andExpect(jsonPath("$.paths['/api/v1/items'].get.parameters[?(@.name == 'favoriteOnly')].schema.default")
                        .value(contains(false)))
                .andExpect(jsonPath("$.paths['/api/v1/items'].get.parameters[3].name")
                        .value("sort"))
                .andExpect(jsonPath("$.paths['/api/v1/items'].get.parameters[3].schema.default")
                        .value("NAME_ASC"))
                .andExpect(jsonPath("$.paths['/api/v1/items'].get.parameters[3].schema.enum")
                        .value(contains("NAME_ASC", "PRICE_ASC", "PRICE_DESC")))
                .andExpect(jsonPath("$.paths['/api/v1/items'].get.responses['200'].content['application/json'].schema.$ref")
                        .value("#/components/schemas/ItemPageResponse"))
                .andExpect(jsonPath("$.components.schemas.ItemResponse.properties.defaultUnit")
                        .exists())
                .andExpect(jsonPath("$.components.schemas.ItemResponse.properties.defaultUnit.type")
                        .value(contains("string", "null")))
                .andExpect(jsonPath("$.components.schemas.ItemResponse.properties.priceDiffRate")
                        .exists())
                .andExpect(jsonPath("$.paths['/api/v1/items'].get.responses['400'].description")
                        .value("조회 조건이 올바르지 않다"))
                .andExpect(jsonPath("$.paths['/api/v1/items'].get.responses['401'].description")
                        .value("찜한 품목만 조회하려면 로그인이 필요하다"))
                .andExpect(jsonPath("$.components.schemas.ItemResponse.properties.isLiked.type")
                        .value("boolean"));
    }

    private MockHttpServletRequestBuilder itemListRequest() {
        return get("/api/v1/items")
                .queryParam("regionId", REGION_ID)
                .queryParam("sort", "NAME_ASC")
                .queryParam("page", "0")
                .queryParam("size", "6");
    }

    private User saveUser(final String name) {
        return userJpaRepository.save(User.oauth(
                ProviderType.KAKAO,
                UUID.randomUUID().toString(),
                UUID.randomUUID() + "@example.com",
                name));
    }

    private void addFavorite(final Long userId, final Long itemId) {
        jdbcTemplate.update(
                "INSERT INTO item_favorites (user_id, item_id) VALUES (?, ?)", userId, itemId);
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
}
