package com.example.demo.news.infrastructure;

import com.example.demo.news.application.port.NewsArticlePort;
import com.example.demo.news.domain.NewsArticle;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class NewsArticlePersistenceAdapter implements NewsArticlePort {

    private final NewsArticleJpaRepository newsArticleJpaRepository;

    @Override
    public void upsertAll(final List<NewsArticle> newsArticles) {
        newsArticles.forEach(this::upsert);
    }

    @Override
    public List<NewsArticle> findLatest(final int size) {
        return newsArticleJpaRepository.findAllByOrderByPublishedAtDescNewsIdDesc(PageRequest.of(0, size));
    }

    private void upsert(final NewsArticle newsArticle) {
        final NewsArticle target = newsArticleJpaRepository
                .findByOriginalUrl(newsArticle.originalUrl())
                .orElse(newsArticle);
        if (target != newsArticle) {
            target.updateFrom(newsArticle);
        }
        newsArticleJpaRepository.save(target);
    }
}
