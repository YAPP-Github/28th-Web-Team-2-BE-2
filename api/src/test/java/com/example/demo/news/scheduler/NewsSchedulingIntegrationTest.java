package com.example.demo.news.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.news.application.port.NewsSource;
import com.example.demo.news.domain.NewsArticle;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(properties = "news.crawler.enabled=true")
@Import(NewsFixtureSourceConfiguration.class)
class NewsSchedulingEnabledIntegrationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("delete from news_articles");
    }

    @Test
    void 수집이_활성화되면_애플리케이션_준비_이벤트에서_fixture_기사를_저장한다() {
        assertThat(applicationContext.getBeansOfType(NewsCrawlerScheduler.class)).hasSize(1);
        final Map<String, Object> article = jdbcTemplate.queryForMap(
                "select title, summary from news_articles where original_url = ?",
                "https://news.example.com/fixture");
        assertThat(article.get("title")).isEqualTo("fixture 뉴스");
        assertThat(article.get("summary")).isEqualTo("fixture 요약");
    }
}

@SpringBootTest(properties = "news.crawler.enabled=false")
@Import(NewsFixtureSourceConfiguration.class)
class NewsSchedulingDisabledIntegrationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("delete from news_articles");
    }

    @Test
    void 수집이_비활성화되면_scheduler_빈을_생성하지_않고_저장하지_않는다() {
        assertThat(applicationContext.getBeansOfType(NewsCrawlerScheduler.class)).isEmpty();
        assertThat(jdbcTemplate.queryForObject("select count(*) from news_articles", Integer.class)).isZero();
    }
}

@TestConfiguration(proxyBeanMethods = false)
class NewsFixtureSourceConfiguration {

    @Bean
    @Primary
    NewsSource newsSource() {
        return () -> List.of(new NewsArticle(
                "fixture 뉴스",
                "fixture 요약",
                "https://news.example.com/fixture",
                "https://image.example.com/fixture.jpg",
                Instant.parse("2026-08-17T00:00:00Z")));
    }
}
