package com.example.demo.news.infrastructure;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import com.example.demo.news.application.port.NewsSource;
import com.example.demo.news.domain.NewsArticle;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Slf4j
@Component
class NaverNewsCrawler implements NewsSource {

    private static final String QUERY = "농산물 채소 가격 시세";
    private static final String MOBILE_SEARCH_URL = "https://m.search.naver.com/search.naver?where=m_news&query=";
    private static final String USER_AGENT =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/127.0.0.0 Safari/537.36";
    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");
    private static final Pattern DATE_PATTERN = Pattern.compile("\\d{4}\\.\\d{2}\\.\\d{2}\\.");
    private static final Pattern DATE_TIME_PATTERN =
            Pattern.compile("\\d{4}\\.\\d{2}\\.\\d{2}\\.\\s*[오전후]{2}\\s*\\d{1,2}:\\d{2}");
    private static final String TITLE_SELECTOR =
            "a[data-heatmap-target=\".title\"], a[data-heatmap-target=\".tit\"], .news_tit";
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy.MM.dd.", Locale.KOREAN);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("yyyy.MM.dd. a h:mm")
            .toFormatter(Locale.KOREAN);

    @Override
    public List<NewsArticle> fetch() {
        try {
            final Document document = Jsoup.connect(searchUrl())
                    .userAgent(USER_AGENT)
                    .timeout(5000)
                    .get();
            final List<NewsArticle> newsArticles = parse(document);
            if (newsArticles.isEmpty()) {
                throw newsUnavailable();
            }
            return newsArticles;
        } catch (final IOException exception) {
            log.warn("[News] Naver search request failed errorMessage={}", exception.getMessage());
            throw newsUnavailable();
        }
    }

    private String searchUrl() {
        return MOBILE_SEARCH_URL + URLEncoder.encode(QUERY, StandardCharsets.UTF_8);
    }

    List<NewsArticle> parse(final Document document) {
        final Map<String, NewsArticle> articlesByUrl = new LinkedHashMap<>();
        document.select(TITLE_SELECTOR).stream()
                .map(this::toNewsArticle)
                .flatMap(Optional::stream)
                .forEach(article -> articlesByUrl.putIfAbsent(article.originalUrl(), article));
        return List.copyOf(articlesByUrl.values());
    }

    private Optional<NewsArticle> toNewsArticle(final Element title) {
        final Element article = enclosingArticle(title);
        final Element summary = first(article, ".body, .dsc_wrap .api_txt_lines, .api_txt_lines");
        final Optional<Instant> publishedAt = publishedAt(article);
        if (title == null || summary == null || publishedAt.isEmpty()) {
            return Optional.empty();
        }
        final String originalUrl = firstNonBlank(title.absUrl("href"), title.attr("href"));
        final String titleText = trimToNull(title.text());
        final String summaryText = trimToNull(summary.text());
        if (originalUrl == null
                || !isHttpUrl(originalUrl)
                || titleText == null
                || summaryText == null) {
            return Optional.empty();
        }
        return Optional.of(new NewsArticle(
                limit(titleText, 500),
                limit(summaryText, 2000),
                originalUrl,
                thumbnailUrl(article),
                publishedAt.get()));
    }

    private Element enclosingArticle(final Element title) {
        Element candidate = title;
        for (int depth = 0; depth < 8; depth++) {
            if (hasArticleFields(candidate)) {
                return candidate;
            }
            candidate = candidate.parent();
            if (candidate == null) {
                return title;
            }
        }
        return candidate;
    }

    private boolean hasArticleFields(final Element candidate) {
        if (candidate == null) {
            return false;
        }
        if (candidate.select(TITLE_SELECTOR).size() != 1) {
            return false;
        }
        if (first(candidate, ".body, .dsc_wrap .api_txt_lines, .api_txt_lines") == null) {
            return false;
        }
        return publishedAt(candidate).isPresent();
    }

    private boolean isHttpUrl(final String url) {
        return url.startsWith("http://") || url.startsWith("https://");
    }

    private Optional<Instant> publishedAt(final Element article) {
        return article.select(".sds-comps-profile-info-subtext, .info_group .info").eachText().stream()
                .map(this::extractDateText)
                .flatMap(Optional::stream)
                .map(this::parseDate)
                .flatMap(Optional::stream)
                .findFirst();
    }

    private Optional<String> extractDateText(final String text) {
        final Matcher dateTimeMatcher = DATE_TIME_PATTERN.matcher(text);
        if (dateTimeMatcher.find()) {
            return Optional.of(dateTimeMatcher.group());
        }
        final Matcher dateMatcher = DATE_PATTERN.matcher(text);
        if (dateMatcher.find()) {
            return Optional.of(dateMatcher.group());
        }
        return Optional.empty();
    }

    private Optional<Instant> parseDate(final String text) {
        try {
            final String normalized = text.replaceAll("\\s+", " ").trim();
            if (normalized.contains("오전") || normalized.contains("오후")) {
                return Optional.of(LocalDateTime.parse(normalized, DATE_TIME_FORMATTER)
                        .atZone(KOREA_ZONE)
                        .toInstant());
            }
            return Optional.of(LocalDate.parse(normalized, DATE_FORMATTER)
                    .atStartOfDay(KOREA_ZONE)
                    .toInstant());
        } catch (final DateTimeParseException exception) {
            return Optional.empty();
        }
    }

    private String thumbnailUrl(final Element article) {
        final Element image = first(article, ".img img, .dsc_thumb img");
        if (image == null) {
            return null;
        }
        final String lazySource = firstNonBlank(image.attr("data-lazysrc"), image.absUrl("data-lazysrc"));
        if (lazySource != null) {
            return lazySource;
        }
        return firstNonBlank(image.attr("src"), image.absUrl("src"));
    }

    private String firstNonBlank(final String first, final String second) {
        final String firstValue = trimToNull(first);
        return firstValue != null ? firstValue : trimToNull(second);
    }

    private Element first(final Element article, final String selector) {
        return article.selectFirst(selector);
    }

    private String trimToNull(final String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return text.trim();
    }

    private String limit(final String text, final int maxLength) {
        final String trimmed = text.trim();
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, maxLength);
    }

    private ApiException newsUnavailable() {
        return new ApiException(
                ErrorType.NEWS_UNAVAILABLE.description(),
                ErrorType.NEWS_UNAVAILABLE,
                HttpStatus.SERVICE_UNAVAILABLE);
    }
}
