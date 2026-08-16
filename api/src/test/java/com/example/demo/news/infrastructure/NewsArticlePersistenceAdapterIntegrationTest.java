package com.example.demo.news.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.demo.news.application.usecase.CollectNewsUseCase;
import com.example.demo.news.application.usecase.GetLatestNewsUseCase;
import com.example.demo.news.application.usecase.SaveNewsArticlesUseCase;
import com.example.demo.news.domain.NewsArticle;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class NewsArticlePersistenceAdapterIntegrationTest {

    @Autowired
    private NewsArticlePersistenceAdapter newsArticlePersistenceAdapter;

    @Autowired
    private NewsArticleJpaRepository newsArticleJpaRepository;

    @Autowired
    private SaveNewsArticlesUseCase saveNewsArticlesUseCase;

    @Autowired
    private GetLatestNewsUseCase getLatestNewsUseCase;

    @AfterEach
    void tearDown() {
        newsArticleJpaRepository.deleteAll();
    }

    @Test
    void 같은_원문_URL의_기사를_다시_저장하면_기존_row를_최신_값으로_갱신한다() {
        final String originalUrl = "https://news.example.com/articles/1";

        newsArticlePersistenceAdapter.upsertAll(List.of(article(
                "첫 제목",
                "첫 요약",
                originalUrl,
                "https://image.example.com/first.jpg",
                "2026-08-15T00:00:00Z")));
        newsArticlePersistenceAdapter.upsertAll(List.of(article(
                "수정된 제목",
                "수정된 요약",
                originalUrl,
                "https://image.example.com/updated.jpg",
                "2026-08-16T00:00:00Z")));

        assertThat(newsArticleJpaRepository.count()).isOne();
        assertThat(newsArticleJpaRepository.findByOriginalUrl(originalUrl))
                .get()
                .satisfies(savedArticle -> {
                    assertThat(savedArticle.title()).isEqualTo("수정된 제목");
                    assertThat(savedArticle.summary()).isEqualTo("수정된 요약");
                    assertThat(savedArticle.thumbnailUrl()).isEqualTo("https://image.example.com/updated.jpg");
                    assertThat(savedArticle.publishedAt()).isEqualTo(Instant.parse("2026-08-16T00:00:00Z"));
                });
    }

    @Test
    void 최신_기사_5건만_발행일과_내부_ID_내림차순으로_조회하고_기존_row는_삭제하지_않는다() {
        newsArticlePersistenceAdapter.upsertAll(List.of(
                article("기사 1", "요약 1", "https://news.example.com/articles/1", null, "2026-08-10T00:00:00Z"),
                article("기사 2", "요약 2", "https://news.example.com/articles/2", null, "2026-08-11T00:00:00Z"),
                article("기사 3", "요약 3", "https://news.example.com/articles/3", null, "2026-08-12T00:00:00Z"),
                article("기사 4", "요약 4", "https://news.example.com/articles/4", null, "2026-08-13T00:00:00Z"),
                article("기사 5", "요약 5", "https://news.example.com/articles/5", null, "2026-08-14T00:00:00Z"),
                article("기사 6", "요약 6", "https://news.example.com/articles/6", null, "2026-08-14T00:00:00Z")));

        final List<NewsArticle> latestArticles = newsArticlePersistenceAdapter.findLatest(5);

        assertThat(latestArticles)
                .extracting(NewsArticle::originalUrl)
                .containsExactly(
                        "https://news.example.com/articles/6",
                        "https://news.example.com/articles/5",
                        "https://news.example.com/articles/4",
                        "https://news.example.com/articles/3",
                        "https://news.example.com/articles/2");
        assertThat(newsArticleJpaRepository.count()).isEqualTo(6);
        assertThat(newsArticleJpaRepository.findByOriginalUrl("https://news.example.com/articles/1")).isPresent();
    }

    @Test
    void 수집에_실패해도_기존_뉴스를_조회한다() {
        final NewsArticle existingArticle = article(
                "기존 제목",
                "기존 요약",
                "https://news.example.com/articles/existing",
                null,
                "2026-08-16T00:00:00Z");
        newsArticlePersistenceAdapter.upsertAll(List.of(existingArticle));

        final CollectNewsUseCase failingCollectNewsUseCase = new CollectNewsUseCase(
                () -> {
                    throw new IllegalStateException("crawl failed");
                },
                saveNewsArticlesUseCase);

        assertThatThrownBy(failingCollectNewsUseCase::execute)
                .isInstanceOf(IllegalStateException.class);
        assertThat(getLatestNewsUseCase.execute())
                .singleElement()
                .satisfies(result -> {
                    assertThat(result.title()).isEqualTo("기존 제목");
                    assertThat(result.originalUrl()).isEqualTo("https://news.example.com/articles/existing");
                });
    }

    private NewsArticle article(
            final String title,
            final String summary,
            final String originalUrl,
            final String thumbnailUrl,
            final String publishedAt) {
        return new NewsArticle(title, summary, originalUrl, thumbnailUrl, Instant.parse(publishedAt));
    }
}
