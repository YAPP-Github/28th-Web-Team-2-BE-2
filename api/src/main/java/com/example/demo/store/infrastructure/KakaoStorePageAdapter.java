package com.example.demo.store.infrastructure;

import com.example.demo.store.application.port.StorePageSource;
import java.io.IOException;
import java.net.URI;
import java.util.Objects;
import java.util.function.Function;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

@Component
public class KakaoStorePageAdapter implements StorePageSource {

    private static final String KAKAO_PLACE_HOST = "place.map.kakao.com";
    private static final int TIMEOUT_MILLIS = 5_000;
    private static final int MAX_BODY_SIZE_BYTES = 1_048_576;
    private static final String USER_AGENT =
            "Mozilla/5.0 (compatible; JangbogoStoreDetail/1.0)";

    private final Function<String, Document> documentFetcher;

    public KakaoStorePageAdapter() {
        this(KakaoStorePageAdapter::fetchDocument);
    }

    KakaoStorePageAdapter(final Function<String, Document> documentFetcher) {
        this.documentFetcher = Objects.requireNonNull(documentFetcher);
    }

    @Override
    public String findOgImage(final String placeUrl) {
        final String normalizedPlaceUrl = normalizePlaceUrl(placeUrl);
        if (normalizedPlaceUrl == null) {
            return null;
        }
        try {
            return ogImageUrl(documentFetcher.apply(normalizedPlaceUrl));
        } catch (final RuntimeException exception) {
            return null;
        }
    }

    private static Document fetchDocument(final String placeUrl) {
        try {
            return Jsoup.connect(placeUrl)
                    .userAgent(USER_AGENT)
                    .timeout(TIMEOUT_MILLIS)
                    .maxBodySize(MAX_BODY_SIZE_BYTES)
                    .followRedirects(false)
                    .get();
        } catch (final IOException exception) {
            return null;
        }
    }

    private String normalizePlaceUrl(final String placeUrl) {
        if (placeUrl == null || placeUrl.isBlank()) {
            return null;
        }
        try {
            final URI uri = URI.create(placeUrl.trim());
            if (!isKakaoPlaceUri(uri)) {
                return null;
            }
            return "https://" + KAKAO_PLACE_HOST + uri.getRawPath();
        } catch (final IllegalArgumentException exception) {
            return null;
        }
    }

    private boolean isKakaoPlaceUri(final URI uri) {
        if (uri.getUserInfo() != null || uri.getPort() != -1) {
            return false;
        }
        if (uri.getRawQuery() != null || uri.getRawFragment() != null) {
            return false;
        }
        if (!KAKAO_PLACE_HOST.equalsIgnoreCase(uri.getHost())) {
            return false;
        }
        if (!isHttpScheme(uri.getScheme())) {
            return false;
        }
        final String path = uri.getRawPath();
        return path != null && path.matches("/\\d+");
    }

    private boolean isHttpScheme(final String scheme) {
        return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
    }

    private String ogImageUrl(final Document document) {
        if (document == null) {
            return null;
        }
        final Element image = document.selectFirst("meta[property='og:image']");
        if (image == null) {
            return null;
        }
        return normalizeImageUrl(image.attr("content"));
    }

    private String normalizeImageUrl(final String content) {
        if (content == null || content.isBlank()) {
            return null;
        }
        final String value = withHttpsScheme(content.trim());
        try {
            final URI uri = URI.create(value);
            if (!isHttpScheme(uri.getScheme()) || uri.getHost() == null) {
                return null;
            }
            return uri.toString();
        } catch (final IllegalArgumentException exception) {
            return null;
        }
    }

    private String withHttpsScheme(final String value) {
        if (value.startsWith("//")) {
            return "https:" + value;
        }
        return value;
    }
}
