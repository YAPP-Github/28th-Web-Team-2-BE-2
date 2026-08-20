package com.example.demo.item.domain;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 품목 기준 단위다. {@code items.default_unit}의 문자열을 해석한다.
 *
 * <p>온라인 가격은 crawler가 전 품목을 100g 기준으로 환산해 저장한다. 화면은 품목 기준 단위 하나만 표시하므로(품목 상세의 제목 옆 단위)
 * 조회 시 그 단위로 되돌려야 금액들이 같은 기준이 된다.
 *
 * <p>무게가 아닌 단위({@code 1개}·{@code 1포기})는 개당 무게 데이터가 없어 환산할 수 없다. 억지로 환산하는 대신 환산 불가로 다룬다.
 */
public record ItemUnit(String label, Integer grams) {

    private static final Pattern WEIGHT = Pattern.compile("^(\\d+)\\s*(g|kg)$");
    private static final int GRAMS_PER_KILOGRAM = 1_000;

    public static ItemUnit of(final String defaultUnit) {
        if (defaultUnit == null) {
            return new ItemUnit(null, null);
        }
        final Matcher matcher = WEIGHT.matcher(defaultUnit.strip());
        if (!matcher.matches()) {
            return new ItemUnit(defaultUnit, null);
        }
        return new ItemUnit(defaultUnit, grams(matcher));
    }

    private static int grams(final Matcher matcher) {
        final int amount = Integer.parseInt(matcher.group(1));
        if ("kg".equals(matcher.group(2))) {
            return amount * GRAMS_PER_KILOGRAM;
        }
        return amount;
    }

    /** 무게로 환산할 수 있는 단위인지. {@code 1개}·{@code 1포기}는 불가다. */
    public boolean convertible() {
        return grams != null;
    }

    /**
     * {@code basisGrams} 기준 가격을 이 단위 기준 가격으로 환산한다.
     *
     * <p>환산할 수 없는 단위면 {@code null}이다.
     */
    public Integer convert(final Integer price, final int basisGrams) {
        if (price == null || !convertible()) {
            return null;
        }
        return Math.round((float) price * grams / basisGrams);
    }
}
