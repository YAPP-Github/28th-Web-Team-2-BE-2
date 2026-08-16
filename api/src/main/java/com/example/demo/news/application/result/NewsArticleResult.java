package com.example.demo.news.application.result;

import java.time.Instant;

public record NewsArticleResult(
        String title,
        String summary,
        String originalUrl,
        Instant publishedAt,
        String thumbnailUrl) {}
