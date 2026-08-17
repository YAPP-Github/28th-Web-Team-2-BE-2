package com.example.demo.news.application.usecase;

import com.example.demo.news.application.port.NewsArticlePort;
import com.example.demo.news.domain.NewsArticle;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SaveNewsArticlesUseCase {

    private final NewsArticlePort newsArticlePort;

    @Transactional
    public void execute(final List<NewsArticle> newsArticles) {
        newsArticlePort.upsertAll(newsArticles);
    }
}
