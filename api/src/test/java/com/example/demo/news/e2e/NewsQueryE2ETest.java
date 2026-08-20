package com.example.demo.news.e2e;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class NewsQueryE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("delete from news_articles");
        insertArticle("뉴스 1", "요약 1", "https://news.example.com/1", "https://image.example.com/1.jpg", "2026-08-16T06:00:00Z");
        insertArticle("뉴스 2", "요약 2", "https://news.example.com/2", null, "2026-08-15T06:00:00Z");
        insertArticle("뉴스 3", "요약 3", "https://news.example.com/3", "https://image.example.com/3.jpg", "2026-08-14T06:00:00Z");
        insertArticle("뉴스 4", "요약 4", "https://news.example.com/4", "https://image.example.com/4.jpg", "2026-08-13T06:00:00Z");
        insertArticle("뉴스 5", "요약 5", "https://news.example.com/5", "https://image.example.com/5.jpg", "2026-08-12T06:00:00Z");
        insertArticle("뉴스 6", "요약 6", "https://news.example.com/6", "https://image.example.com/6.jpg", "2026-08-11T06:00:00Z");
    }

    @Test
    void 공개_뉴스_목록의_최신_5개를_공통_응답으로_조회한다() throws Exception {
        mockMvc.perform(get("/api/v1/news"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("요청이 성공적으로 처리되었습니다."))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isNotEmpty())
                .andExpect(jsonPath("$.data.length()").value(5))
                .andExpect(jsonPath("$.data[*].title").value(org.hamcrest.Matchers.contains("뉴스 1", "뉴스 2", "뉴스 3", "뉴스 4", "뉴스 5")))
                .andExpect(jsonPath("$.data[0].summary").value("요약 1"))
                .andExpect(jsonPath("$.data[0].originalUrl").value("https://news.example.com/1"))
                .andExpect(jsonPath("$.data[0].publishedAt").value("2026-08-16T06:00:00Z"))
                .andExpect(jsonPath("$.data[0].thumbnailUrl").value("https://image.example.com/1.jpg"))
                .andExpect(jsonPath("$.data[1].thumbnailUrl").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.data[5]").doesNotExist());
    }

    @Test
    void 저장된_뉴스가_없으면_service_unavailable을_응답한다() throws Exception {
        jdbcTemplate.update("delete from news_articles");

        mockMvc.perform(get("/api/v1/news"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("조회할 뉴스 데이터가 없습니다."));
    }

    private void insertArticle(
            final String title,
            final String summary,
            final String originalUrl,
            final String thumbnailUrl,
            final String publishedAt) {
        jdbcTemplate.update(
                "insert into news_articles (title, summary, original_url, thumbnail_url, published_at) values (?, ?, ?, ?, ?)",
                title,
                summary,
                originalUrl,
                thumbnailUrl,
                Instant.parse(publishedAt));
    }
}
