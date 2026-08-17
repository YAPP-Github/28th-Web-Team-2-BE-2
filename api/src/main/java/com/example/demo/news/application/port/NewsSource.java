package com.example.demo.news.application.port;

import com.example.demo.news.domain.NewsArticle;
import java.util.List;

public interface NewsSource {

    List<NewsArticle> fetch();
}
