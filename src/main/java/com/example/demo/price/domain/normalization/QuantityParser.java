package com.example.demo.price.domain.normalization;

import com.example.demo.price.domain.ParsedQuantity;
import com.example.demo.price.domain.PriceUnit;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
public class QuantityParser {

    private static final Pattern WEIGHT = Pattern.compile(
            "(?<value>\\d+(?:\\.\\d+)?)\\s*(?<unit>kg|킬로그램|g|그램)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern COUNT = Pattern.compile(
            "(?<value>\\d+)\\s*(?<unit>개|입|봉|팩|포기|단)", Pattern.CASE_INSENSITIVE);

    public Optional<ParsedQuantity> parse(final String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        final Matcher weight = WEIGHT.matcher(text.toLowerCase(Locale.ROOT));
        if (weight.find()) {
            return Optional.of(toWeight(weight));
        }
        final Matcher count = COUNT.matcher(text);
        if (count.find()) {
            return Optional.of(new ParsedQuantity(
                    new BigDecimal(count.group("value")), PriceUnit.COUNT));
        }
        return Optional.empty();
    }

    private ParsedQuantity toWeight(final Matcher matcher) {
        final BigDecimal value = new BigDecimal(matcher.group("value"));
        final String unit = matcher.group("unit").toLowerCase(Locale.ROOT);
        if (unit.equals("kg") || unit.equals("킬로그램")) {
            return new ParsedQuantity(value, PriceUnit.KG);
        }
        return new ParsedQuantity(value, PriceUnit.G);
    }
}
