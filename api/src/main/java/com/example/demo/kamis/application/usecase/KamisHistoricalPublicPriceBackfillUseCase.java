package com.example.demo.kamis.application.usecase;

import com.example.demo.kamis.application.port.KamisItemCatalogPort;
import com.example.demo.kamis.application.port.KamisItemCatalogPort.KamisItem;
import com.example.demo.kamis.application.port.KamisPeriodPriceQueryPort;
import com.example.demo.kamis.application.port.KamisPriceQueryPort;
import com.example.demo.kamis.application.port.PublicPriceCommandPort;
import com.example.demo.kamis.application.port.PublicPriceCommandPort.PublicPriceCommand;
import com.example.demo.kamis.application.query.KamisDailyPriceQuery;
import com.example.demo.kamis.application.query.KamisPeriodPriceQuery;
import com.example.demo.kamis.application.result.KamisDailyPriceItemResult;
import com.example.demo.kamis.application.result.KamisPeriodPriceItemResult;
import com.example.demo.kamis.application.result.KamisPeriodPriceResult;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KamisHistoricalPublicPriceBackfillUseCase {

    private static final List<String> ITEM_CATEGORY_CODES = List.of("100", "200", "300", "400", "500", "600");
    private static final String PRODUCT_CLASS_CODE = "02";
    private static final String CONVERT_KG_YES = "Y";
    private static final String PRODUCT_RANK = "04";
    private static final String MID_PRODUCT_RANK = "05";

    private final KamisPriceQueryPort kamisPriceQueryPort;
    private final KamisPeriodPriceQueryPort kamisPeriodPriceQueryPort;
    private final KamisItemCatalogPort kamisItemCatalogPort;
    private final PublicPriceCommandPort publicPriceCommandPort;

    public int execute(
            final String regionId,
            final String countryCode,
            final LocalDate startDate,
            final LocalDate endDate) {
        return execute(List.of(regionId), countryCode, startDate, endDate);
    }

    public int execute(
            final List<String> regionIds,
            final String countryCode,
            final LocalDate startDate,
            final LocalDate endDate) {
        validateRange(startDate, endDate);
        validateRegionIds(regionIds);
        final List<KamisItem> catalog = kamisItemCatalogPort.findAll();
        int saved = 0;
        for (final String categoryCode : ITEM_CATEGORY_CODES) {
            final List<PublicPriceCandidate> prices = new ArrayList<>();
            collectCategoryPrices(categoryCode, countryCode, startDate, endDate, catalog, prices);
            if (!prices.isEmpty()) {
                saved += persistPrices(regionIds, dedupe(prices));
            }
        }
        return saved;
    }

    private void collectCategoryPrices(
            final String categoryCode,
            final String countryCode,
            final LocalDate startDate,
            final LocalDate endDate,
            final List<KamisItem> catalog,
            final List<PublicPriceCandidate> prices) {
        final KamisDailyPriceQuery query = new KamisDailyPriceQuery(
                PRODUCT_CLASS_CODE, categoryCode, countryCode, null, CONVERT_KG_YES);
        final List<KamisDailyPriceItemResult> itemVariants = kamisPriceQueryPort.findDailyPrices(query).items();
        for (final KamisDailyPriceItemResult itemVariant : itemVariants) {
            collectVariantPrices(categoryCode, countryCode, startDate, endDate, catalog, itemVariant, prices);
        }
    }

    private void collectVariantPrices(
            final String categoryCode,
            final String countryCode,
            final LocalDate startDate,
            final LocalDate endDate,
            final List<KamisItem> catalog,
            final KamisDailyPriceItemResult itemVariant,
            final List<PublicPriceCandidate> prices) {
        if (!isQueryable(itemVariant)) {
            return;
        }
        final List<KamisItem> matchedItems = matchingItems(itemVariant, catalog);
        if (matchedItems.isEmpty()) {
            return;
        }
        final KamisPeriodPriceResult result = kamisPeriodPriceQueryPort.findWholesalePeriodPrices(
                new KamisPeriodPriceQuery(
                        categoryCode,
                        itemVariant.itemCode(),
                        itemVariant.kindCode(),
                        productRankCode(itemVariant.rank()),
                        countryCode,
                        startDate,
                        endDate,
                        CONVERT_KG_YES));
        addCandidates(result.items(), matchedItems, itemVariant.unit(), startDate, endDate, prices);
    }

    private void addCandidates(
            final List<KamisPeriodPriceItemResult> results,
            final List<KamisItem> matchedItems,
            final String fallbackUnit,
            final LocalDate startDate,
            final LocalDate endDate,
            final List<PublicPriceCandidate> prices) {
        for (final KamisPeriodPriceItemResult result : results) {
            final LocalDate priceDate = parseDate(result);
            if (priceDate == null || priceDate.isBefore(startDate) || priceDate.isAfter(endDate)) {
                continue;
            }
            for (final KamisItem item : matchedItems) {
                final Integer itemPrice = normalizedPrice(result, fallbackUnit, item.defaultUnit());
                if (itemPrice != null) {
                    prices.add(new PublicPriceCandidate(item.itemId(), itemPrice, priceDate));
                }
            }
        }
    }

    private List<KamisItem> matchingItems(
            final KamisDailyPriceItemResult result, final List<KamisItem> catalog) {
        final List<KamisItem> normalizedMatches = catalog.stream()
                .filter(item -> normalized(item.itemName()).equals(normalized(result.itemName())))
                .toList();
        if (!normalizedMatches.isEmpty()) {
            return normalizedMatches;
        }
        final String kindName = normalized(kindName(result.kindName()));
        if (kindName.isBlank()) {
            return List.of();
        }
        return catalog.stream()
                .filter(item -> normalized(item.itemName()).contains(normalized(result.itemName())))
                .filter(item -> normalized(item.itemName()).contains(kindName))
                .toList();
    }

    private boolean isQueryable(final KamisDailyPriceItemResult result) {
        return !isBlank(result.itemCode()) && !isBlank(productRankCode(result.rank()));
    }

    private String productRankCode(final String rank) {
        if ("상품".equals(rank)) {
            return PRODUCT_RANK;
        }
        if ("중품".equals(rank)) {
            return MID_PRODUCT_RANK;
        }
        return null;
    }

    private Integer normalizedPrice(
            final KamisPeriodPriceItemResult result,
            final String fallbackUnit,
            final String defaultUnit) {
        final Integer price = parsePrice(result.price());
        final String unit = firstNonBlank(result.unit(), fallbackUnit);
        if (price == null || defaultUnit == null || unit == null) {
            return null;
        }
        if ("1kg".equals(defaultUnit)) {
            return isWeightUnit(unit) ? price : null;
        }
        if ("100g".equals(defaultUnit)) {
            return isWeightUnit(unit)
                    ? BigDecimal.valueOf(price).divide(BigDecimal.TEN, 0, RoundingMode.HALF_UP).intValue()
                    : null;
        }
        if (defaultUnit.equals(unit)) {
            return price;
        }
        return null;
    }

    private LocalDate parseDate(final KamisPeriodPriceItemResult result) {
        final String regDay = result.regDay();
        if (isBlank(regDay)) {
            return null;
        }
        final String normalizedRegDay = regDay.trim().replace('/', '-').replace('.', '-');
        if (normalizedRegDay.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return parse(normalizedRegDay, DateTimeFormatter.ISO_LOCAL_DATE);
        }
        if (!isBlank(result.year()) && normalizedRegDay.matches("\\d{2}-\\d{2}")) {
            return parse(result.year().trim() + "-" + normalizedRegDay, DateTimeFormatter.ISO_LOCAL_DATE);
        }
        if (normalizedRegDay.matches("\\d{8}")) {
            return parse(normalizedRegDay, DateTimeFormatter.BASIC_ISO_DATE);
        }
        return null;
    }

    private LocalDate parse(final String value, final DateTimeFormatter formatter) {
        try {
            return LocalDate.parse(value, formatter);
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private int persistPrices(
            final List<String> regionIds, final List<PublicPriceCandidate> prices) {
        return regionIds.stream()
                .mapToInt(regionId -> publicPriceCommandPort.upsertAll(toCommands(prices, regionId)))
                .sum();
    }

    private List<PublicPriceCommand> toCommands(
            final List<PublicPriceCandidate> prices, final String regionId) {
        return prices.stream()
                .map(price -> new PublicPriceCommand(price.itemId(), regionId, price.price(), price.priceDate()))
                .toList();
    }

    private List<PublicPriceCandidate> dedupe(final List<PublicPriceCandidate> prices) {
        // ponytail: keep one representative row per item/date; add market history only with a schema dimension.
        return prices.stream()
                .collect(Collectors.groupingBy(
                        price -> price.itemId() + ":" + price.priceDate(),
                        Collectors.minBy(Comparator.comparingInt(PublicPriceCandidate::price))))
                .values().stream()
                .flatMap(java.util.Optional::stream)
                .toList();
    }

    private String kindName(final String value) {
        if (isBlank(value)) {
            return "";
        }
        final int parenthesis = value.indexOf('(');
        if (parenthesis < 0) {
            return value;
        }
        return value.substring(0, parenthesis);
    }

    private String normalized(final String value) {
        if (isBlank(value)) {
            return "";
        }
        return value.replace(" ", "")
                .replace("-", "")
                .replace("(", "")
                .replace(")", "")
                .trim();
    }

    private String firstNonBlank(final String preferred, final String fallback) {
        if (!isBlank(preferred)) {
            return preferred;
        }
        return fallback;
    }

    private boolean isWeightUnit(final String unit) {
        return unit.matches("\\d+(?:kg|g)");
    }

    private Integer parsePrice(final String value) {
        if (isBlank(value) || "-".equals(value.trim())) {
            return null;
        }
        try {
            return Integer.parseInt(value.replace(",", "").trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private boolean isBlank(final String value) {
        return value == null || value.isBlank();
    }

    private void validateRange(final LocalDate startDate, final LocalDate endDate) {
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("KAMIS backfill date range is invalid");
        }
        if (startDate.isBefore(endDate.minusYears(1).plusDays(1))) {
            throw new IllegalArgumentException("KAMIS backfill range must not exceed one year");
        }
    }

    private void validateRegionIds(final List<String> regionIds) {
        if (regionIds == null || regionIds.isEmpty() || regionIds.stream().anyMatch(this::isBlank)) {
            throw new IllegalArgumentException("KAMIS backfill region IDs are invalid");
        }
    }

    private record PublicPriceCandidate(Long itemId, int price, LocalDate priceDate) {}
}
