package com.example.demo.item.infrastructure.crawler;

import com.example.demo.external.selenium.SeleniumDriverFactory;
import com.example.demo.external.selenium.SeleniumPage;
import com.example.demo.external.selenium.SeleniumPageLoadException;
import com.example.demo.item.application.command.CrawlRequest;
import com.example.demo.item.application.port.ItemCrawler;
import com.example.demo.item.application.result.CrawledPage;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SeleniumItemCrawlerAdapter implements ItemCrawler {

    private final SeleniumDriverFactory driverFactory;

    @Override
    public CrawledPage crawl(final CrawlRequest request) {
        try {
            final SeleniumPage page = driverFactory.loadPage(request.targetUrl());
            return CrawledPage.success(page.sourceUrl(), page.html(), OffsetDateTime.now());
        } catch (SeleniumPageLoadException exception) {
            return CrawledPage.temporaryFailure(
                    request.targetUrl(), OffsetDateTime.now(), exception.getMessage());
        }
    }
}
