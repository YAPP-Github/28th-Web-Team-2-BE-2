package com.example.demo.store.infrastructure;

import com.example.demo.store.application.port.StorePageSource;
import com.example.demo.store.application.result.StorePageContent;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

@Component
public class KakaoStorePageAdapter implements StorePageSource {

    private static final String KAKAO_PLACE_HOST = "place.map.kakao.com";
    private static final int TIMEOUT_MILLIS = 5_000;
    private static final int MAX_BODY_SIZE_BYTES = 1_048_576;
    private static final int MAX_IMAGE_SIZE_BYTES = 5 * 1024 * 1024;
    private static final String USER_AGENT =
            "Mozilla/5.0 (compatible; JangbogoStoreDetail/1.0)";
    private static final String BUSINESS_HOURS_ROWS = """
            .list_operation li,
            .list_operation .line,
            .info_operation li,
            .info_operation .line,
            [class*='operation'] li,
            [class*='operation'] .line,
            [class*='hours'] li,
            [class*='hours'] .line,
            [class*='business'] li,
            [class*='business'] .line,
            [data-day],
            [data-testid*='hour']
            """;
    private static final Pattern DAY_PATTERN =
            Pattern.compile("(?:월|화|수|목|금|토|일)(?:요일)?|매일");
    private static final Pattern TIME_PATTERN =
            Pattern.compile("(?:\\d{1,2}:\\d{2}|24시간)");
    private static final Pattern OPEN_PATTERN = Pattern.compile("영업\\s*중");
    private static final Pattern CLOSED_PATTERN =
            Pattern.compile("영업\\s*(?:종료|준비중)|오늘\\s*휴무|휴무일");
    private static final Set<String> ALLOWED_IMAGE_HOSTS =
            Set.of("kakao.com", "kakaocdn.net", "daumcdn.net");

    private final Function<String, Document> documentFetcher;
    private final Function<String, DownloadedImage> imageFetcher;

    public KakaoStorePageAdapter() {
        this(KakaoStorePageAdapter::fetchDocument, KakaoStorePageAdapter::fetchImage);
    }

    KakaoStorePageAdapter(final Function<String, Document> documentFetcher) {
        this(documentFetcher, url -> null);
    }

    KakaoStorePageAdapter(
            final Function<String, Document> documentFetcher,
            final Function<String, DownloadedImage> imageFetcher) {
        this.documentFetcher = Objects.requireNonNull(documentFetcher);
        this.imageFetcher = Objects.requireNonNull(imageFetcher);
    }

    @Override
    public StorePageContent find(final String placeUrl) {
        final String normalizedPlaceUrl = normalizePlaceUrl(placeUrl);
        if (normalizedPlaceUrl == null) {
            return StorePageContent.empty();
        }
        try {
            final Document document = documentFetcher.apply(normalizedPlaceUrl);
            if (document == null) {
                return StorePageContent.empty();
            }
            final String imageUrl = ogImageUrl(document);
            final DownloadedImage image = downloadImage(imageUrl);
            return new StorePageContent(
                    imageUrl,
                    imageContentType(image),
                    imageContent(image),
                    businessHours(document),
                    openStatus(document));
        } catch (final RuntimeException exception) {
            return StorePageContent.empty();
        }
    }

    public String findOgImage(final String placeUrl) {
        return find(placeUrl).imageUrl();
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

    private static DownloadedImage fetchImage(final String imageUrl) {
        HttpURLConnection connection = null;
        try {
            final URL url = URI.create(imageUrl).toURL();
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(TIMEOUT_MILLIS);
            connection.setReadTimeout(TIMEOUT_MILLIS);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestProperty("User-Agent", USER_AGENT);
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return null;
            }
            final byte[] content = readImage(connection);
            if (content == null) {
                return null;
            }
            return new DownloadedImage(mediaType(connection.getContentType()), content);
        } catch (final IOException | IllegalArgumentException exception) {
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static byte[] readImage(final HttpURLConnection connection) throws IOException {
        try (InputStream input = connection.getInputStream()) {
            final ByteArrayOutputStream output = new ByteArrayOutputStream();
            final byte[] buffer = new byte[8192];
            int size = 0;
            int read;
            while ((read = input.read(buffer)) != -1) {
                size += read;
                if (size > MAX_IMAGE_SIZE_BYTES) {
                    return null;
                }
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
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
            if (uri.getUserInfo() != null || uri.getPort() != -1 || uri.getRawFragment() != null) {
                return null;
            }
            if (!isAllowedImageHost(uri.getHost())) {
                return null;
            }
            return uri.toString();
        } catch (final IllegalArgumentException exception) {
            return null;
        }
    }

    private boolean isAllowedImageHost(final String host) {
        final String normalizedHost = host.toLowerCase(Locale.ROOT);
        return ALLOWED_IMAGE_HOSTS.stream()
                .anyMatch(allowed -> normalizedHost.equals(allowed)
                        || normalizedHost.endsWith("." + allowed));
    }

    private String withHttpsScheme(final String value) {
        if (value.startsWith("//")) {
            return "https:" + value;
        }
        return value;
    }

    private DownloadedImage downloadImage(final String imageUrl) {
        if (imageUrl == null) {
            return null;
        }
        try {
            return imageFetcher.apply(imageUrl);
        } catch (final RuntimeException exception) {
            return null;
        }
    }

    private String imageContentType(final DownloadedImage image) {
        if (image == null) {
            return null;
        }
        return image.contentType();
    }

    private byte[] imageContent(final DownloadedImage image) {
        if (image == null) {
            return null;
        }
        return image.content();
    }

    private List<String> businessHours(final Document document) {
        final LinkedHashSet<String> hours = new LinkedHashSet<>();
        for (final Element row : document.select(BUSINESS_HOURS_ROWS)) {
            final String text = normalizeText(Jsoup.parse(row.html().replace("><", "> <")).text());
            if (looksLikeBusinessHours(text)) {
                hours.add(text);
            }
        }
        return List.copyOf(hours);
    }

    private boolean looksLikeBusinessHours(final String text) {
        return (DAY_PATTERN.matcher(text).find() || text.contains("영업시간"))
                && (TIME_PATTERN.matcher(text).find() || text.contains("휴무"));
    }

    private String openStatus(final Document document) {
        String text = normalizeText(document.select(
                ".txt_operation, .txt_status, [class*='status'], [class*='open'], [data-testid*='status']")
                .text());
        if (text.isBlank()) {
            text = normalizeText(document.text());
        }
        if (OPEN_PATTERN.matcher(text).find()) {
            return "OPEN";
        }
        if (CLOSED_PATTERN.matcher(text).find()) {
            return "CLOSED";
        }
        return "UNKNOWN";
    }

    private String normalizeText(final String text) {
        return text == null ? "" : text.trim().replaceAll("\\s+", " ");
    }

    private static String mediaType(final String value) {
        if (value == null) {
            return null;
        }
        final int separator = value.indexOf(';');
        if (separator < 0) {
            return value.trim();
        }
        return value.substring(0, separator).trim();
    }

    record DownloadedImage(String contentType, byte[] content) {}
}
