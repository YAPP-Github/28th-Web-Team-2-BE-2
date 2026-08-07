package com.example.demo.price.domain.normalization;

import com.example.demo.price.domain.NormalizedPrice;
import com.example.demo.price.domain.ParsedQuantity;
import com.example.demo.price.domain.PriceUnit;
import com.example.demo.price.domain.RawOffer;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
public class PriceNormalizer {

    private static final BigDecimal GRAMS_PER_KILOGRAM = BigDecimal.valueOf(1000);
    private static final BigDecimal GRAMS_PER_PRICE_UNIT = BigDecimal.valueOf(100);

    public Optional<NormalizedPrice> normalize(final RawOffer offer, final PriceUnit targetUnit) {
        if (!offer.available() || offer.salePrice() == null || offer.quantity() == null) {
            return Optional.empty();
        }
        final BigDecimal comparableQuantity = comparableQuantity(offer.quantity(), targetUnit);
        if (comparableQuantity == null || comparableQuantity.signum() <= 0) {
            return Optional.empty();
        }
        final BigDecimal totalPrice = offer.salePrice().add(offer.shippingFee());
        return Optional.of(new NormalizedPrice(
                totalPrice.divide(comparableQuantity, 2, RoundingMode.HALF_UP), targetUnit,
                pricePer100g(totalPrice, offer.quantity())));
    }

    private BigDecimal comparableQuantity(final ParsedQuantity quantity, final PriceUnit targetUnit) {
        if (targetUnit == PriceUnit.KG && quantity.unit() == PriceUnit.KG) {
            return quantity.value();
        }
        if (targetUnit == PriceUnit.KG && quantity.unit() == PriceUnit.G) {
            return quantity.value().divide(GRAMS_PER_KILOGRAM, 6, RoundingMode.HALF_UP);
        }
        if (targetUnit == PriceUnit.G && quantity.unit() == PriceUnit.KG) {
            return quantity.value().multiply(GRAMS_PER_KILOGRAM);
        }
        if (targetUnit == quantity.unit()) {
            return quantity.value();
        }
        return null;
    }

    private BigDecimal pricePer100g(final BigDecimal totalPrice, final ParsedQuantity quantity) {
        final BigDecimal grams = grams(quantity);
        if (grams == null || grams.signum() <= 0) {
            return null;
        }
        return totalPrice.multiply(GRAMS_PER_PRICE_UNIT)
                .divide(grams, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal grams(final ParsedQuantity quantity) {
        if (quantity.unit() == PriceUnit.G) {
            return quantity.value();
        }
        if (quantity.unit() == PriceUnit.KG) {
            return quantity.value().multiply(GRAMS_PER_KILOGRAM);
        }
        return null;
    }
}
