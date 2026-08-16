package com.example.demo.news.presentation;

import com.example.demo.news.application.usecase.GetLatestNewsUseCase;
import com.example.demo.news.presentation.dto.NewsArticleResponse;
import com.example.demo.news.presentation.spec.NewsControllerSpec;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/news")
@RequiredArgsConstructor
public class NewsController implements NewsControllerSpec {

    private final GetLatestNewsUseCase getLatestNewsUseCase;

    @GetMapping
    @Override
    public ResponseEntity<List<NewsArticleResponse>> getNews() {
        final List<NewsArticleResponse> response = getLatestNewsUseCase.execute().stream()
                .map(NewsArticleResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }
}
