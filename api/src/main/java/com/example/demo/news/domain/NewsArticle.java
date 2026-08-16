package com.example.demo.news.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Entity
@Table(name = "news_articles")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Accessors(fluent = true)
public class NewsArticle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "news_id")
    private Long newsId;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(nullable = false, length = 2000)
    private String summary;

    @Column(name = "original_url", nullable = false, unique = true, length = 2048)
    private String originalUrl;

    @Column(name = "thumbnail_url", length = 2048)
    private String thumbnailUrl;

    @Column(name = "published_at", nullable = false)
    private Instant publishedAt;

    public NewsArticle(
            final String title,
            final String summary,
            final String originalUrl,
            final String thumbnailUrl,
            final Instant publishedAt) {
        validateRequired(title, summary, originalUrl, publishedAt);
        this.title = title;
        this.summary = summary;
        this.originalUrl = originalUrl;
        this.thumbnailUrl = thumbnailUrl;
        this.publishedAt = publishedAt;
    }

    public void updateFrom(final NewsArticle newsArticle) {
        title = newsArticle.title();
        summary = newsArticle.summary();
        thumbnailUrl = newsArticle.thumbnailUrl();
        publishedAt = newsArticle.publishedAt();
    }

    private void validateRequired(
            final String title,
            final String summary,
            final String originalUrl,
            final Instant publishedAt) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("news title must not be blank");
        }
        if (summary == null || summary.isBlank()) {
            throw new IllegalArgumentException("news summary must not be blank");
        }
        if (originalUrl == null || originalUrl.isBlank()) {
            throw new IllegalArgumentException("news original url must not be blank");
        }
        if (publishedAt == null) {
            throw new IllegalArgumentException("news published at must not be null");
        }
    }
}
