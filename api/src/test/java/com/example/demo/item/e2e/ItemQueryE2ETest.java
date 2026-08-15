package com.example.demo.item.e2e;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.item.domain.Item;
import com.example.demo.item.domain.PublicPrice;
import com.example.demo.item.infrastructure.ItemJpaRepository;
import com.example.demo.item.infrastructure.PublicPriceJpaRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ItemQueryE2ETest {

    private static final String REGION_ID = "1121510100";
    private static final String OTHER_REGION_ID = "1168010100";

    private final MockMvc mockMvc;
    private final ItemJpaRepository itemJpaRepository;
    private final PublicPriceJpaRepository publicPriceJpaRepository;
    private LocalDate referenceDate;

    @Autowired
    ItemQueryE2ETest(
            final MockMvc mockMvc,
            final ItemJpaRepository itemJpaRepository,
            final PublicPriceJpaRepository publicPriceJpaRepository) {
        this.mockMvc = mockMvc;
        this.itemJpaRepository = itemJpaRepository;
        this.publicPriceJpaRepository = publicPriceJpaRepository;
    }

    @BeforeEach
    void setUp() {
        publicPriceJpaRepository.deleteAll();
        itemJpaRepository.deleteAll();
        referenceDate = LocalDate.now();

        final Item potato = itemJpaRepository.save(new Item("감자", "1kg"));
        final Item onion = itemJpaRepository.save(new Item("양파", "1kg"));
        final Item greenOnion = itemJpaRepository.save(new Item("대파", "1단"));
        final Item carrot = itemJpaRepository.save(new Item("당근", "1kg"));
        itemJpaRepository.save(new Item("양배추", "1통"));
        publicPriceJpaRepository.save(
                new PublicPrice(potato.id(), REGION_ID, 3000, referenceDate.minusDays(2)));
        publicPriceJpaRepository.save(new PublicPrice(potato.id(), REGION_ID, 3500, referenceDate));
        publicPriceJpaRepository.save(
                new PublicPrice(onion.id(), REGION_ID, 3000, referenceDate.minusDays(3)));
        publicPriceJpaRepository.save(new PublicPrice(onion.id(), REGION_ID, 2800, referenceDate));
        publicPriceJpaRepository.save(
                new PublicPrice(greenOnion.id(), REGION_ID, 2100, referenceDate.minusDays(4)));
        publicPriceJpaRepository.save(new PublicPrice(greenOnion.id(), REGION_ID, 2100, referenceDate));
        publicPriceJpaRepository.save(
                new PublicPrice(carrot.id(), REGION_ID, 4000, referenceDate.minusDays(6)));
        publicPriceJpaRepository.save(
                new PublicPrice(carrot.id(), REGION_ID, 4500, referenceDate.minusDays(5)));
        publicPriceJpaRepository.save(
                new PublicPrice(carrot.id(), REGION_ID, 4600, referenceDate.minusDays(5)));
        publicPriceJpaRepository.save(new PublicPrice(potato.id(), OTHER_REGION_ID, 9999, referenceDate));
    }

    @Test
    void 공개_품목과_공공가격_목록을_직접_성공_응답과_priceGap으로_조회한다() throws Exception {
        mockMvc.perform(get("/api/v1/items")
                        .queryParam("regionId", REGION_ID)
                        .queryParam("page", "0")
                        .queryParam("size", "5"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").doesNotExist())
                .andExpect(jsonPath("$.message").doesNotExist())
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.baseDate").value(referenceDate.toString()))
                .andExpect(jsonPath("$.totalCount").value(5))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].price").value(3500))
                .andExpect(jsonPath("$.items[0].priceGap").value(500))
                .andExpect(jsonPath("$.items[1].price").value(2800))
                .andExpect(jsonPath("$.items[1].priceGap").value(-200))
                .andExpect(jsonPath("$.items[2].price").value(2100))
                .andExpect(jsonPath("$.items[2].priceGap").value(0))
                .andExpect(jsonPath("$.items[3].price").value(4600))
                .andExpect(jsonPath("$.items[3].priceGap").value(600))
                .andExpect(jsonPath("$.items[4].price").value(nullValue()))
                .andExpect(jsonPath("$.items[4].priceGap").value(nullValue()))
                .andExpect(jsonPath("$.items[0].priceDiffRate").doesNotExist())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.hasNext").isBoolean());
    }

    @Test
    void 페이지_크기를_생략하면_10개를_기본값으로_사용한다() throws Exception {
        mockMvc.perform(get("/api/v1/items")
                        .queryParam("regionId", REGION_ID))
                .andExpect(status().isOk())
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
    void 품목_조회_경로와_페이지_파라미터를_OpenAPI에_노출한다() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/items'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/items'].get.parameters[?(@.name == 'regionId')]")
                        .isNotEmpty())
                .andExpect(jsonPath("$.paths['/api/v1/items'].get.parameters[?(@.name == 'page')]")
                        .isNotEmpty())
                .andExpect(jsonPath("$.paths['/api/v1/items'].get.parameters[?(@.name == 'size')]")
                        .isNotEmpty())
                .andExpect(jsonPath("$.paths['/api/v1/items'].get.responses['200'].content['application/json'].schema.$ref")
                        .value("#/components/schemas/ItemPageResponse"))
                .andExpect(jsonPath("$.paths['/api/v1/items'].get.responses['400']").exists());
    }
}
