package com.example.demo.item.infrastructure.crawler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.external.selenium.SeleniumDriverFactory;
import com.example.demo.external.selenium.SeleniumPage;
import com.example.demo.external.selenium.SeleniumPageLoadException;
import com.example.demo.item.application.command.CrawlRequest;
import com.example.demo.item.application.result.CrawledPage;
import com.example.demo.item.application.result.CrawledPage.CrawlStatus;
import java.net.URI;
import org.junit.jupiter.api.Test;

class SeleniumItemCrawlerAdapterTest {

    private static final URI TARGET_URL = URI.create("https://example.com/items?query=apple");

    @Test
    void loadsPageSourceAndClosesDriver() {
        final SeleniumDriverFactory driverFactory = mock(SeleniumDriverFactory.class);
        final SeleniumItemCrawlerAdapter adapter = new SeleniumItemCrawlerAdapter(driverFactory);
        when(driverFactory.loadPage(TARGET_URL))
                .thenReturn(new SeleniumPage(TARGET_URL, "<html>items</html>"));

        final CrawledPage result = adapter.crawl(new CrawlRequest(TARGET_URL));

        assertThat(result.status()).isEqualTo(CrawlStatus.SUCCESS);
        assertThat(result.html()).isEqualTo("<html>items</html>");
        assertThat(result.sourceUrl()).isEqualTo(TARGET_URL);
        verify(driverFactory).loadPage(TARGET_URL);
    }

    @Test
    void returnsTemporaryFailureWhenSeleniumFails() {
        final SeleniumDriverFactory driverFactory = mock(SeleniumDriverFactory.class);
        final SeleniumItemCrawlerAdapter adapter = new SeleniumItemCrawlerAdapter(driverFactory);
        when(driverFactory.loadPage(TARGET_URL))
                .thenThrow(new SeleniumPageLoadException(
                        TARGET_URL, "page load timeout", new RuntimeException("timeout")));

        final CrawledPage result = adapter.crawl(new CrawlRequest(TARGET_URL));

        assertThat(result.status()).isEqualTo(CrawlStatus.TEMPORARY_FAILURE);
        assertThat(result.sourceUrl()).isEqualTo(TARGET_URL);
        assertThat(result.failureReason()).isEqualTo("page load timeout");
        verify(driverFactory).loadPage(TARGET_URL);
    }
}
