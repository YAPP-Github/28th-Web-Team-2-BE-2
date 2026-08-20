package com.example.demo.kamis.application.usecase;

import com.example.demo.kamis.application.port.KamisItemCatalogPort;
import com.example.demo.kamis.application.port.KamisItemCatalogPort.KamisItem;
import com.example.demo.kamis.application.port.KamisPriceQueryPort;
import com.example.demo.kamis.application.port.PublicPriceCommandPort;
import com.example.demo.kamis.application.port.PublicPriceCommandPort.PublicPriceCommand;
import com.example.demo.kamis.application.query.KamisDailyPriceQuery;
import com.example.demo.kamis.application.result.KamisDailyPriceItemResult;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CollectKamisPublicPriceUseCase {

    private static final List<String> ITEM_CATEGORY_CODES = List.of("100", "200", "300", "400", "500", "600");
    private static final String PRODUCT_CLASS_CODE = "02";
    private static final String SEOUL_COUNTRY_CODE = "1101";
    private static final String CONVERT_KG_YES = "Y";

    private final KamisPriceQueryPort kamisPriceQueryPort;
    private final KamisItemCatalogPort kamisItemCatalogPort;
    private final PublicPriceCommandPort publicPriceCommandPort;

    public int execute(final String regionId, final String countryCode, final LocalDate priceDate) {
        final Map<String, List<KamisItem>> itemsByName = kamisItemCatalogPort.findAll().stream()
                .collect(Collectors.groupingBy(KamisItem::itemName));
        final List<PublicPriceCommand> prices = new ArrayList<>();
        for (final String categoryCode : ITEM_CATEGORY_CODES) {
            final KamisDailyPriceQuery query = new KamisDailyPriceQuery(
                    PRODUCT_CLASS_CODE, categoryCode, countryCode, priceDate, CONVERT_KG_YES);
            final List<KamisDailyPriceItemResult> results = kamisPriceQueryPort.findDailyPrices(query).items();
            prices.addAll(toCommands(results, itemsByName, regionId, priceDate));
        }
        return publicPriceCommandPort.upsertAll(dedupe(prices));
    }

    public int execute(final String regionId, final LocalDate priceDate) {
        return execute(regionId, SEOUL_COUNTRY_CODE, priceDate);
    }

    private List<PublicPriceCommand> toCommands(
            final List<KamisDailyPriceItemResult> results,
            final Map<String, List<KamisItem>> itemsByName,
            final String regionId,
            final LocalDate priceDate) {
        return results.stream()
                .filter(result -> result.itemName() != null)
                .filter(result -> itemsByName.containsKey(result.itemName()))
                .filter(result -> parsePrice(result.dpr1()) != null)
                .flatMap(result -> itemsByName.get(result.itemName()).stream()
                        .filter(item -> normalizedPrice(result, item.defaultUnit()) != null)
                        .map(item -> new PublicPriceCommand(
                                item.itemId(), regionId, normalizedPrice(result, item.defaultUnit()), priceDate)))
                .toList();
    }

    private List<PublicPriceCommand> dedupe(final List<PublicPriceCommand> prices) {
        return prices.stream()
                .collect(Collectors.groupingBy(
                        command -> command.itemId() + ":" + command.regionId() + ":" + command.priceDate(),
                        Collectors.minBy(Comparator.comparingInt(PublicPriceCommand::price))))
                .values().stream()
                .flatMap(java.util.Optional::stream)
                .toList();
    }

    private Integer normalizedPrice(final KamisDailyPriceItemResult result, final String defaultUnit) {
        final Integer price = parsePrice(result.dpr1());
        if (price == null || defaultUnit == null || result.unit() == null) {
            return null;
        }
        if ("1kg".equals(defaultUnit)) {
            return isWeightUnit(result.unit()) ? price : null;
        }
        if ("100g".equals(defaultUnit)) {
            return isWeightUnit(result.unit()) ? BigDecimal.valueOf(price)
                    .divide(BigDecimal.TEN, 0, RoundingMode.HALF_UP).intValue() : null;
        }
        if (defaultUnit.equals(result.unit())) {
            return price;
        }
        return null;
    }

    private boolean isWeightUnit(final String unit) {
        return unit.matches("\\d+(?:kg|g)");
    }

    private Integer parsePrice(final String value) {
        if (value == null || value.isBlank() || "-".equals(value.trim())) {
            return null;
        }
        try {
            return Integer.parseInt(value.replace(",", "").trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
