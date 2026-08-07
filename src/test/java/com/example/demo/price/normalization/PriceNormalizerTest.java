package com.example.demo.price.domain.normalization;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.price.domain.NormalizedPrice;
import com.example.demo.price.domain.ParsedQuantity;
import com.example.demo.price.domain.PriceUnit;
import com.example.demo.price.domain.RawOffer;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PriceNormalizerTest {

    private final PriceNormalizer normalizer = new PriceNormalizer();

    @Test
    void 일반가와_필수_배송비를_킬로그램_단가에_반영한다() {
        final RawOffer offer = new RawOffer(
                "p-1", "감자 500g", "https://example.com/p-1", BigDecimal.valueOf(4000),
                BigDecimal.valueOf(1000), new ParsedQuantity(BigDecimal.valueOf(500), PriceUnit.G),
                "국내산", true, false);

        final NormalizedPrice result = normalizer.normalize(offer, PriceUnit.KG).orElseThrow();

        assertThat(result.amount()).isEqualByComparingTo("10000.00");
        assertThat(result.pricePer100g()).isEqualByComparingTo("1000.00");
    }

    @Test
    void 그램_상품도_백그램당_가격을_계산한다() {
        final RawOffer offer = new RawOffer(
                "p-1", "시금치 200g", "https://example.com/p-1", BigDecimal.valueOf(3000),
                BigDecimal.ZERO, new ParsedQuantity(BigDecimal.valueOf(200), PriceUnit.G),
                null, true, false);

        final NormalizedPrice result = normalizer.normalize(offer, PriceUnit.G).orElseThrow();

        assertThat(result.amount()).isEqualByComparingTo("15.00");
        assertThat(result.pricePer100g()).isEqualByComparingTo("1500.00");
    }

    @Test
    void 수량이_없으면_정규화하지_않는다() {
        final RawOffer offer = new RawOffer(
                "p-1", "감자", "https://example.com/p-1", BigDecimal.valueOf(4000),
                BigDecimal.ZERO, null, "국내산", true, false);

        assertThat(normalizer.normalize(offer, PriceUnit.KG)).isEmpty();
    }

    @Test
    void 같은_개수_단위는_개당_가격으로_정규화한다() {
        final RawOffer offer = new RawOffer(
                "p-1", "감자 2개", "https://example.com/p-1", BigDecimal.valueOf(4000),
                BigDecimal.ZERO, new ParsedQuantity(BigDecimal.valueOf(2), PriceUnit.COUNT),
                "국내산", true, false);

        assertThat(normalizer.normalize(offer, PriceUnit.COUNT).orElseThrow().amount())
                .isEqualByComparingTo("2000.00");
    }

    @Test
    void 서로_변환할_수_없는_단위는_제외한다() {
        final RawOffer offer = new RawOffer(
                "p-1", "감자 2개", "https://example.com/p-1", BigDecimal.valueOf(4000),
                BigDecimal.ZERO, new ParsedQuantity(BigDecimal.valueOf(2), PriceUnit.COUNT),
                "국내산", true, false);

        assertThat(normalizer.normalize(offer, PriceUnit.KG)).isEmpty();
    }
}
