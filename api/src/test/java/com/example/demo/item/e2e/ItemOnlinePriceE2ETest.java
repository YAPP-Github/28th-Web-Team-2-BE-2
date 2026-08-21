package com.example.demo.item.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.item.domain.Item;
import com.example.demo.item.domain.ItemCategory;
import com.example.demo.item.domain.OnlineChannel;
import com.example.demo.item.domain.OnlinePrice;
import com.example.demo.item.infrastructure.ItemJpaRepository;
import com.example.demo.item.infrastructure.OnlineChannelJpaRepository;
import com.example.demo.item.infrastructure.OnlinePriceJpaRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ItemOnlinePriceE2ETest {

    private static final int PER_100_GRAMS = 100;

    private final MockMvc mockMvc;
    private final ItemJpaRepository itemJpaRepository;
    private final OnlinePriceJpaRepository onlinePriceJpaRepository;
    private final OnlineChannelJpaRepository onlineChannelJpaRepository;
    private Long potatoId;
    private Long onionId;
    private LocalDate today;
    private int oasis;
    private int kurly;
    private int elevenSt;
    private int gsShop;

    @Autowired
    ItemOnlinePriceE2ETest(
            final MockMvc mockMvc,
            final ItemJpaRepository itemJpaRepository,
            final OnlinePriceJpaRepository onlinePriceJpaRepository,
            final OnlineChannelJpaRepository onlineChannelJpaRepository) {
        this.mockMvc = mockMvc;
        this.itemJpaRepository = itemJpaRepository;
        this.onlinePriceJpaRepository = onlinePriceJpaRepository;
        this.onlineChannelJpaRepository = onlineChannelJpaRepository;
    }

    @BeforeEach
    void setUp() {
        onlinePriceJpaRepository.deleteAll();
        onlineChannelJpaRepository.deleteAll();
        itemJpaRepository.deleteAll();
        today = LocalDate.now();
        // 테스트 스키마는 Flyway 를 쓰지 않아 online_channels 시드가 없다. 운영과 같은 순서로 만든다.
        oasis = onlineChannelJpaRepository.save(new OnlineChannel("오아시스", "새벽배송")).id();
        kurly = onlineChannelJpaRepository.save(new OnlineChannel("컬리", "새벽배송")).id();
        elevenSt = onlineChannelJpaRepository.save(new OnlineChannel("11번가", "오픈마켓")).id();
        gsShop = onlineChannelJpaRepository.save(new OnlineChannel("GS SHOP", "오픈마켓")).id();
        potatoId = itemJpaRepository.save(
                new Item("감자", "1kg", null, ItemCategory.ROOT_VEGETABLES)).id();
        onionId = itemJpaRepository.save(
                new Item("양파", "1kg", null, ItemCategory.SEASONINGS)).id();
    }

    private void save(final int channelId, final int price, final LocalDate collectedAt) {
        save(channelId, price, collectedAt, PER_100_GRAMS);
    }

    private void save(
            final int channelId, final int price, final LocalDate collectedAt, final int unit) {
        onlinePriceJpaRepository.save(new OnlinePrice(
                potatoId, channelId, "감자", "감자 " + price + "원", price, unit,
                "https://example.com/" + channelId + "/" + price, "무료배송", collectedAt));
    }

    @Test
    @DisplayName("최신 수집일의 채널별 최저가를 채널 순으로 반환한다")
    void returnsLowestPricePerChannel() throws Exception {
        save(oasis, 390, today);
        save(oasis, 320, today);
        save(kurly, 450, today);
        save(elevenSt, 280, today);
        save(gsShop, 360, today);

        mockMvc.perform(get(path(potatoId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemId").value(potatoId))
                .andExpect(jsonPath("$.onlinePrices[*].channelId").value(contains(oasis, kurly, elevenSt, gsShop)))
                .andExpect(jsonPath("$.onlinePrices[*].channelName")
                        .value(contains("오아시스", "컬리", "11번가", "GS SHOP")))
                .andExpect(jsonPath("$.onlinePrices[*].channelKind")
                        .value(contains("새벽배송", "새벽배송", "오픈마켓", "오픈마켓")))
                .andExpect(jsonPath("$.onlinePrices[*].price").value(contains(3200, 4500, 2800, 3600)))
                .andExpect(jsonPath("$.onlinePrices[*].deliveryNote")
                        .value(contains("무료배송", "무료배송", "무료배송", "무료배송")))
                .andExpect(jsonPath("$.onlinePrices[*].productUrl")
                        .value(contains(
                                "https://example.com/" + oasis + "/320",
                                "https://example.com/" + kurly + "/450",
                                "https://example.com/" + elevenSt + "/280",
                                "https://example.com/" + gsShop + "/360")))
                .andExpect(jsonPath("$.onlinePrices[0].productName").value("감자 320원"))
                .andExpect(jsonPath("$.onlinePrices[0].unit").value("1kg"))
                .andExpect(jsonPath("$.onlinePrices[0].channelKind").value("새벽배송"))
                .andExpect(jsonPath("$.onlinePrices[0].deliveryNote").value("무료배송"))
                .andExpect(jsonPath("$.onlinePrices[0].productUrl")
                        .value("https://example.com/" + oasis + "/320"))
                .andExpect(jsonPath("$.onlinePrices[0].collectedAt").value(today.toString()));
    }

    @Test
    @DisplayName("이전 수집일 가격은 섞이지 않는다")
    void excludesOlderCollectionDates() throws Exception {
        save(oasis, 100, today.minusDays(1));
        save(kurly, 450, today);

        mockMvc.perform(get(path(potatoId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.onlinePrices[*].price").value(contains(4500)))
                .andExpect(jsonPath("$.onlinePrices[*].collectedAt").value(contains(today.toString())));
    }

    @Test
    @DisplayName("100g 기준이 아닌 수집 행은 비교 대상에서 제외한다")
    void excludesRowsOutsideNormalizedUnit() throws Exception {
        save(oasis, 900, today, 500);
        save(kurly, 450, today);

        mockMvc.perform(get(path(potatoId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.onlinePrices[*].channelId").value(contains(kurly)));
    }

    @Test
    @DisplayName("지원하지 않는 온라인 채널은 반환하지 않는다")
    void excludesUnsupportedChannels() throws Exception {
        final int unsupportedChannel = onlineChannelJpaRepository.save(new OnlineChannel("온라인몰")).id();
        save(oasis, 320, today);
        save(unsupportedChannel, 100, today.plusDays(1));

        mockMvc.perform(get(path(potatoId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.onlinePrices[*].channelId").value(contains(oasis)))
                .andExpect(jsonPath("$.onlinePrices[*].channelName").value(contains("오아시스")));
    }

    @Test
    @DisplayName("무게로 환산할 수 없는 단위는 수집 기준인 100g을 그대로 쓴다")
    void keepsHundredGramsForNonWeightUnit() throws Exception {
        final Long watermelonId = itemJpaRepository.save(
                new Item("수박", "1개", null, ItemCategory.FRUITS)).id();
        onlinePriceJpaRepository.save(new OnlinePrice(
                watermelonId, kurly, "수박", "수박 한 통", 450, PER_100_GRAMS,
                "https://example.com/w", "새벽배송", today));

        mockMvc.perform(get(path(watermelonId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.onlinePrices[0].price").value(450))
                .andExpect(jsonPath("$.onlinePrices[0].unit").value("100g"));
    }

    @Test
    @DisplayName("수집 데이터가 없으면 200과 빈 목록을 반환한다")
    void returnsEmptyListWhenNoData() throws Exception {
        mockMvc.perform(get(path(onionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemId").value(onionId))
                .andExpect(jsonPath("$.onlinePrices").isEmpty());
    }

    @Test
    @DisplayName("존재하지 않는 품목은 404를 반환한다")
    void returnsNotFoundForUnknownItem() throws Exception {
        mockMvc.perform(get(path(999_999L))).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("잘못된 품목 ID는 400을 반환한다")
    void returnsBadRequestForInvalidItemId() throws Exception {
        mockMvc.perform(get(path(0L))).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("온라인 가격 조회는 저장된 가격을 변경하지 않는다")
    void doesNotMutateOnlinePrices() throws Exception {
        save(oasis, 320, today);
        final long countBefore = onlinePriceJpaRepository.count();

        mockMvc.perform(get(path(potatoId))).andExpect(status().isOk());

        assertThat(onlinePriceJpaRepository.count()).isEqualTo(countBefore);
    }

    @Test
    @DisplayName("온라인 가격 API가 OpenAPI 문서에 노출된다")
    void exposesApiDocs() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/items/{itemId}/online-prices'].get").exists())
                .andExpect(jsonPath(
                                "$.paths['/api/v1/items/{itemId}/online-prices'].get.responses['400'].description")
                        .value("조회 조건이 올바르지 않다"))
                .andExpect(jsonPath("$.components.schemas.ItemOnlinePriceResponse.properties.itemId").exists())
                .andExpect(jsonPath("$.components.schemas.ItemOnlinePriceResponse.properties.onlinePrices")
                        .exists())
                .andExpect(jsonPath("$.components.schemas.OnlineChannelPriceResponse.properties.productUrl")
                        .exists())
                .andExpect(jsonPath("$.components.schemas.OnlineChannelPriceResponse.properties.channelKind.enum")
                        .value(contains("새벽배송", "당일배송", "오픈마켓", "즉시배송")))
                .andExpect(jsonPath("$.components.schemas.OnlineChannelPriceResponse.required")
                        .value(contains("channelKind")))
                .andExpect(jsonPath("$.components.schemas.OnlineChannelPriceResponse.properties.deliveryNote")
                        .exists());
    }

    private String path(final Long itemId) {
        return "/api/v1/items/" + itemId + "/online-prices";
    }
}
