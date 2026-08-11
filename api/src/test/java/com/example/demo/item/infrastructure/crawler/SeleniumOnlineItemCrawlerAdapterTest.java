package com.example.demo.item.infrastructure.crawler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import com.example.demo.external.selenium.SeleniumPage;
import com.example.demo.external.selenium.factory.SeleniumDriverFactory;
import com.example.demo.item.application.command.CrawlOnlineItemCommand;
import com.example.demo.item.application.result.CrawlOnlineItemResult;
import com.example.demo.item.application.result.OnlineItemCrawlStatus;
import java.net.URI;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class SeleniumOnlineItemCrawlerAdapterTest {

    private static final URI TARGET_URL = URI.create("https://example.com/items?query=apple");

    @Test
    void convertsLoadedPageToSuccessfulCrawlOnlineItemResult() {
        final SeleniumDriverFactory driverFactory = mock(SeleniumDriverFactory.class);
        final SeleniumOnlineItemCrawlerAdapter adapter = new SeleniumOnlineItemCrawlerAdapter(driverFactory);
        when(driverFactory.loadPage(TARGET_URL))
                .thenReturn(new SeleniumPage(TARGET_URL, "<html>items</html>"));

        final CrawlOnlineItemResult result = adapter.crawl(new CrawlOnlineItemCommand(TARGET_URL));

        assertThat(result.status()).isEqualTo(OnlineItemCrawlStatus.SUCCESS);
        assertThat(result.html()).isEqualTo("<html>items</html>");
        assertThat(result.sourceUrl()).isEqualTo(TARGET_URL);
        verify(driverFactory).loadPage(TARGET_URL);
    }

    @Test
    void convertsExternalFailureToTemporaryFailure() {
        final SeleniumDriverFactory driverFactory = mock(SeleniumDriverFactory.class);
        final SeleniumOnlineItemCrawlerAdapter adapter = new SeleniumOnlineItemCrawlerAdapter(driverFactory);
        when(driverFactory.loadPage(TARGET_URL))
                .thenThrow(new ApiException(
                        ErrorType.EXTERNAL_API_ERROR.description(),
                        ErrorType.EXTERNAL_API_ERROR,
                        HttpStatus.BAD_GATEWAY));

        final CrawlOnlineItemResult result = adapter.crawl(new CrawlOnlineItemCommand(TARGET_URL));

        assertThat(result.status()).isEqualTo(OnlineItemCrawlStatus.TEMPORARY_FAILURE);
        assertThat(result.sourceUrl()).isEqualTo(TARGET_URL);
        assertThat(result.failureReason()).isEqualTo(ErrorType.EXTERNAL_API_ERROR.description());
        verify(driverFactory).loadPage(TARGET_URL);
    }
}
