package com.example.demo.news.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.news.domain.NewsArticle;
import java.time.Instant;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

class NaverNewsCrawlerTest {

    private final NaverNewsCrawler crawler = new NaverNewsCrawler();

    @Test
    void 모바일_검색_결과의_mark_텍스트와_절대_원문_URL과_발행일을_파싱한다() {
        final List<NewsArticle> articles = crawler.parse(document("""
                <article>
                  <a data-heatmap-target=".title" href="/article/202608170001"><mark>배추</mark> 가격</a>
                  <div class="body">오늘 <mark>배추</mark> 시세</div>
                  <div class="img"><img src="https://image.example.com/cabbage.jpg"></div>
                  <span class="sds-comps-profile-info-subtext">2026.08.17.</span>
                </article>
                """));

        assertThat(articles).singleElement().satisfies(article -> {
            assertThat(article.title()).isEqualTo("배추 가격");
            assertThat(article.summary()).isEqualTo("오늘 배추 시세");
            assertThat(article.originalUrl()).isEqualTo("https://m.search.naver.com/article/202608170001");
            assertThat(article.thumbnailUrl()).isEqualTo("https://image.example.com/cabbage.jpg");
            assertThat(article.publishedAt()).isEqualTo(Instant.parse("2026-08-16T15:00:00Z"));
        });
    }

    @Test
    void 썸네일이_없는_기사도_파싱한다() {
        final List<NewsArticle> articles = crawler.parse(document("""
                <article>
                  <a data-heatmap-target=".title" href="https://news.example.com/without-thumbnail">썸네일 없는 기사</a>
                  <div class="body">본문 요약</div>
                  <span class="sds-comps-profile-info-subtext">2026.08.17.</span>
                </article>
                """));

        assertThat(articles).singleElement().satisfies(article ->
                assertThat(article.thumbnailUrl()).isNull());
    }

    @Test
    void 상대경로_data_lazysrc를_절대_URL로_파싱한다() {
        final List<NewsArticle> articles = crawler.parse(document("""
                <article>
                  <a data-heatmap-target=".title" href="https://news.example.com/lazy">지연 이미지 기사</a>
                  <div class="body">요약</div>
                  <div class="img"><img data-lazysrc="/images/lazy.jpg" src="/images/fallback.jpg"></div>
                  <span class="sds-comps-profile-info-subtext">2026.08.17.</span>
                </article>
                """));

        assertThat(articles).singleElement()
                .extracting(NewsArticle::thumbnailUrl)
                .isEqualTo("https://m.search.naver.com/images/lazy.jpg");
    }

    @Test
    void 상대경로_src를_절대_URL로_파싱한다() {
        final List<NewsArticle> articles = crawler.parse(document("""
                <article>
                  <a data-heatmap-target=".title" href="https://news.example.com/src">src 이미지 기사</a>
                  <div class="body">요약</div>
                  <div class="img"><img src="/images/src.jpg"></div>
                  <span class="sds-comps-profile-info-subtext">2026.08.17.</span>
                </article>
                """));

        assertThat(articles).singleElement()
                .extracting(NewsArticle::thumbnailUrl)
                .isEqualTo("https://m.search.naver.com/images/src.jpg");
    }

    @Test
    void 필수_요약이나_발행일이_없는_기사는_제외한다() {
        final List<NewsArticle> articles = crawler.parse(document("""
                <article>
                  <a data-heatmap-target=".title" href="https://news.example.com/without-summary">요약 없는 기사</a>
                  <span class="sds-comps-profile-info-subtext">2026.08.17.</span>
                </article>
                <article>
                  <a data-heatmap-target=".title" href="https://news.example.com/without-date">발행일 없는 기사</a>
                  <div class="body">본문 요약</div>
                </article>
                """));

        assertThat(articles).isEmpty();
    }

    @Test
    void 같은_원문_URL의_기사는_하나만_파싱한다() {
        final List<NewsArticle> articles = crawler.parse(document("""
                <article>
                  <a data-heatmap-target=".title" href="https://news.example.com/duplicate">첫 기사</a>
                  <div class="body">첫 요약</div>
                  <span class="sds-comps-profile-info-subtext">2026.08.17.</span>
                </article>
                <article>
                  <a data-heatmap-target=".title" href="https://news.example.com/duplicate">두 번째 기사</a>
                  <div class="body">두 번째 요약</div>
                  <span class="sds-comps-profile-info-subtext">2026.08.16.</span>
                </article>
                """));

        assertThat(articles).singleElement().satisfies(article -> {
            assertThat(article.title()).isEqualTo("첫 기사");
            assertThat(article.originalUrl()).isEqualTo("https://news.example.com/duplicate");
        });
    }

    private Document document(final String html) {
        return Jsoup.parse(html, "https://m.search.naver.com/");
    }
}
