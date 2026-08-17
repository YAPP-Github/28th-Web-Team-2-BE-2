package com.example.demo.news.application.usecase;

import com.example.demo.news.application.port.NewsSource;
import com.example.demo.news.domain.NewsArticle;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CollectNewsUseCase {

    private final NewsSource newsSource;
    private final SaveNewsArticlesUseCase saveNewsArticlesUseCase;

    public void execute() {
        final List<NewsArticle> newsArticles = newsSource.fetch();
        saveNewsArticlesUseCase.execute(newsArticles);
    }
}
