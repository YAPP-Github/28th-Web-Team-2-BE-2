package com.example.demo.store.infrastructure;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

record KakaoStoreDetailParserResult(String imageUrl, List<String> businessHours, String openStatus) {}

final class KakaoStoreDetailParser {

    private KakaoStoreDetailParser() {}

    static KakaoStoreDetailParserResult parse(final Document document) {
        return new KakaoStoreDetailParserResult(
                imageUrl(document), businessHours(document), openStatus(document));
    }

    private static String imageUrl(final Document document) {
        for (final Element image : document.select("img[src], img[data-src], meta[property=og:image]")) {
            final String url = image.hasAttr("content")
                    ? image.attr("content")
                    : image.hasAttr("src") ? image.absUrl("src") : image.absUrl("data-src");
            if (url.toLowerCase(Locale.ROOT).contains("org")) {
                return url;
            }
        }
        return null;
    }

    private static List<String> businessHours(final Document document) {
        final List<String> hours = new ArrayList<>();
        for (final Element element : document.select(".list_operation li, .txt_operation, [class*=operation]")) {
            final String text = element.text().trim();
            if (!text.isBlank() && !hours.contains(text)) {
                hours.add(text);
            }
        }
        return hours.isEmpty() ? null : List.copyOf(hours);
    }

    private static String openStatus(final Document document) {
        final String text = document.select(".open_state, .txt_operation, [class*=open]")
                .stream().map(Element::text).map(String::trim).filter(value -> !value.isBlank())
                .findFirst().orElse(null);
        if (text == null) {
            return null;
        }
        if (text.contains("영업중")) {
            return "OPEN";
        }
        if (text.contains("영업종료") || text.contains("휴무")) {
            return "CLOSED";
        }
        return null;
    }
}
