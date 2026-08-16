package com.example.demo.news.infrastructure;

import com.example.demo.news.domain.NewsArticle;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface NewsArticleJpaRepository extends JpaRepository<NewsArticle, Long> {

    Optional<NewsArticle> findByOriginalUrl(String originalUrl);

    List<NewsArticle> findTop5ByOrderByPublishedAtDescNewsIdDesc();
}
