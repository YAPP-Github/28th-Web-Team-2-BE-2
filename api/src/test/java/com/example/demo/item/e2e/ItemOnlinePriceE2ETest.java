package com.example.demo.item.e2e;

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
        oasis = onlineChannelJpaRepository.save(new OnlineChannel("오아시스")).id();
        kurly = onlineChannelJpaRepository.save(new OnlineChannel("컬리")).id();
        elevenSt = onlineChannelJpaRepository.save(new OnlineChannel("11번가")).id();
        onlineChannelJpaRepository.save(new OnlineChannel("GS SHOP"));
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

        mockMvc.perform(get(path(potatoId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemId").value(potatoId))
                .andExpect(jsonPath("$.onlinePrices[*].channelId").value(contains(oasis, kurly, elevenSt)))
                .andExpect(jsonPath("$.onlinePrices[*].channelName")
                        .value(contains("오아시스", "컬리", "11번가")))
                .andExpect(jsonPath("$.onlinePrices[*].price").value(contains(320, 450, 280)))
                .andExpect(jsonPath("$.onlinePrices[0].productName").value("감자 320원"))
                .andExpect(jsonPath("$.onlinePrices[0].quantity").value(1))
                .andExpect(jsonPath("$.onlinePrices[0].unit").value("g"))
                .andExpect(jsonPath("$.onlinePrices[0].normalizedPrice").value(320))
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
                .andExpect(jsonPath("$.onlinePrices[*].price").value(contains(450)))
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
    @DisplayName("온라인 가격 API가 OpenAPI 문서에 노출된다")
    void exposesApiDocs() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/items/{itemId}/online-prices'].get").exists());
    }

    private String path(final Long itemId) {
        return "/api/v1/items/" + itemId + "/online-prices";
    }
}
