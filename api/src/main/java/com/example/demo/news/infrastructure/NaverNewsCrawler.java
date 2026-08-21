package com.example.demo.news.infrastructure;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import com.example.demo.news.application.port.NewsSource;
import com.example.demo.news.domain.NewsArticle;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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
    private static final Pattern RELATIVE_DATE_PATTERN =
            Pattern.compile("(?<amount>\\d+)\\s*(?<unit>분|시간|일|주|개월|년)\\s*전");
    private static final String TITLE_SELECTOR =
            "a[data-heatmap-target=\".title\"], a[data-heatmap-target=\".tit\"], .news_tit";
    private static final String SUMMARY_SELECTOR =
            ".body, a[data-heatmap-target=\".body\"], .dsc_wrap .api_txt_lines, .api_txt_lines";
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy.MM.dd.", Locale.KOREAN);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("yyyy.MM.dd. a h:mm")
            .toFormatter(Locale.KOREAN);

    @Override
    public List<NewsArticle> fetch() {
        // ponytail: static demo feed; replace with an approved news API after demo day.
        return List.of(
                new NewsArticle(
                        "충남 아산·홍성 동부에 폭염주의보 해제",
                        "기상청이 21일 오전 아산과 홍성 동부의 폭염주의보를 해제했습니다. 논산·공주·부여·금산·청양에는 폭염주의보가 유지됩니다.",
                        "https://www.yna.co.kr/amp/view/AKR20260821053200527",
                        null,
                        Instant.parse("2026-08-21T01:02:00Z")),
                new NewsArticle(
                        "“처서 매직 없다니 이게 무슨 날벼락”…주말 소나기 지나면 찜통더위 [주말 날씨]",
                        "처서 주말에도 비와 소나기가 잠시 더위를 식히는 데 그치고, 다음 주까지 높은 기온과 습도가 이어질 전망입니다.",
                        "https://m.mk.co.kr/amp/12133150",
                        null,
                        Instant.parse("2026-08-21T05:20:41Z")),
                new NewsArticle(
                        "폭염에도 사과값 안정…출하 20.5%↑·소매가 28.6%↓",
                        "폭염과 가뭄에도 사과·배 수급 영향은 제한적이며, 사과 출하량 증가로 소매가격은 안정세를 보였습니다.",
                        "https://m.newspim.com/news/view/20260820000935",
                        null,
                        Instant.parse("2026-08-20T00:00:00Z")),
                new NewsArticle(
                        "폭염에 채솟값 뛰고 가공식품도 줄인상…8월 장바구니 물가 부담",
                        "폭염으로 시금치·배추·양배추·깻잎·상추 등 일부 채소의 소매가격이 상승했고, 애호박과 오이 가격도 올랐습니다.",
                        "https://www.yna.co.kr/view/AKR20260812047400030",
                        null,
                        Instant.parse("2026-08-12T00:00:00Z")),
                new NewsArticle(
                        "배추·무 등 여름 채소 공급 안정…추석까지 가격 낮을 듯",
                        "양호한 작황과 출하량 증가로 배추·무·양배추 가격이 평년보다 낮고, 큰 기상이변이 없으면 추석까지 공급이 안정될 전망입니다.",
                        "https://www.imaeil.com/page/view/2026081113532545452",
                        null,
                        Instant.parse("2026-08-11T04:53:37Z")));
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
        final Element summary = first(article, SUMMARY_SELECTOR);
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
        if (first(candidate, SUMMARY_SELECTOR) == null) {
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
        if (text.contains("방금 전")) {
            return Optional.of("방금 전");
        }
        final Matcher relativeDateMatcher = RELATIVE_DATE_PATTERN.matcher(text);
        if (relativeDateMatcher.find()) {
            return Optional.of(relativeDateMatcher.group());
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
            if (DATE_PATTERN.matcher(normalized).find()) {
                return Optional.of(LocalDate.parse(normalized, DATE_FORMATTER)
                        .atStartOfDay(KOREA_ZONE)
                        .toInstant());
            }
            return parseRelativeDate(normalized);
        } catch (final DateTimeParseException exception) {
            return Optional.empty();
        }
    }

    private Optional<Instant> parseRelativeDate(final String text) {
        if ("방금 전".equals(text)) {
            return Optional.of(Instant.now());
        }
        final Matcher matcher = RELATIVE_DATE_PATTERN.matcher(text);
        if (!matcher.matches()) {
            return Optional.empty();
        }
        try {
            final long amount = Long.parseLong(matcher.group("amount"));
            final ZonedDateTime now = ZonedDateTime.now(KOREA_ZONE);
            return switch (matcher.group("unit")) {
                case "분" -> Optional.of(now.minusMinutes(amount).toInstant());
                case "시간" -> Optional.of(now.minusHours(amount).toInstant());
                case "일" -> Optional.of(now.minusDays(amount).toInstant());
                case "주" -> Optional.of(now.minusWeeks(amount).toInstant());
                case "개월" -> Optional.of(now.minusMonths(amount).toInstant());
                case "년" -> Optional.of(now.minusYears(amount).toInstant());
                default -> Optional.empty();
            };
        } catch (final NumberFormatException | DateTimeException | ArithmeticException exception) {
            return Optional.empty();
        }
    }

    private String thumbnailUrl(final Element article) {
        final Element image = first(article, ".img img, .dsc_thumb img, a[data-heatmap-target=\".thumb\"] img");
        if (image == null) {
            return null;
        }
        final String lazySource = firstNonBlank(image.absUrl("data-lazysrc"), image.attr("data-lazysrc"));
        if (lazySource != null) {
            return lazySource;
        }
        return firstNonBlank(image.absUrl("src"), image.attr("src"));
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
