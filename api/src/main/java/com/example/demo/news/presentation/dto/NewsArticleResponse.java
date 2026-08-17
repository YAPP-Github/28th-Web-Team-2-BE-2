package com.example.demo.news.presentation.dto;

import com.example.demo.news.application.result.NewsArticleResult;
import java.time.Instant;

public record NewsArticleResponse(
        String title,
        String summary,
        String originalUrl,
        Instant publishedAt,
        String thumbnailUrl) {

    public static NewsArticleResponse from(final NewsArticleResult result) {
        return new NewsArticleResponse(
                result.title(),
                result.summary(),
                result.originalUrl(),
                result.publishedAt(),
                result.thumbnailUrl());
    }
}
