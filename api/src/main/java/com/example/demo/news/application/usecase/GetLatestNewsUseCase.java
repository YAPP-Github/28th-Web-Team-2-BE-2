package com.example.demo.news.application.usecase;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import com.example.demo.news.application.port.NewsArticlePort;
import com.example.demo.news.application.result.NewsArticleResult;
import com.example.demo.news.domain.NewsArticle;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetLatestNewsUseCase {

    private static final int NEWS_SIZE = 5;

    private final NewsArticlePort newsArticlePort;

    @Transactional(readOnly = true)
    public List<NewsArticleResult> execute() {
        final List<NewsArticle> newsArticles = newsArticlePort.findLatest(NEWS_SIZE);
        if (newsArticles.isEmpty()) {
            throw newsUnavailable();
        }
        return newsArticles.stream().map(this::toResult).toList();
    }

    private NewsArticleResult toResult(final NewsArticle newsArticle) {
        return new NewsArticleResult(
                newsArticle.title(),
                newsArticle.summary(),
                newsArticle.originalUrl(),
                newsArticle.publishedAt(),
                newsArticle.thumbnailUrl());
    }

    private ApiException newsUnavailable() {
        return new ApiException(
                ErrorType.NEWS_UNAVAILABLE.description(),
                ErrorType.NEWS_UNAVAILABLE,
                HttpStatus.SERVICE_UNAVAILABLE);
    }
}
