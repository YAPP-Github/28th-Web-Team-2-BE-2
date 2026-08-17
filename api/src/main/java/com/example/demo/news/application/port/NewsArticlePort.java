package com.example.demo.news.application.port;

import com.example.demo.news.domain.NewsArticle;
import java.util.List;

public interface NewsArticlePort {

    void upsertAll(List<NewsArticle> newsArticles);

    List<NewsArticle> findLatest(int size);
}
