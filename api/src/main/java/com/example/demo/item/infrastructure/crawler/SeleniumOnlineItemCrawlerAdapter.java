package com.example.demo.item.infrastructure.crawler;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import com.example.demo.external.selenium.SeleniumPage;
import com.example.demo.external.selenium.factory.SeleniumDriverFactory;
import com.example.demo.item.application.command.CrawlOnlineItemCommand;
import com.example.demo.item.application.port.OnlineItemCrawlerPort;
import com.example.demo.item.application.result.CrawlOnlineItemResult;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SeleniumOnlineItemCrawlerAdapter implements OnlineItemCrawlerPort {

    private final SeleniumDriverFactory driverFactory;

    @Override
    public CrawlOnlineItemResult crawl(final CrawlOnlineItemCommand command) {
        try {
            final SeleniumPage page = driverFactory.loadPage(command.targetUrl());
            return CrawlOnlineItemResult.success(page.sourceUrl(), page.html(), OffsetDateTime.now());
        } catch (ApiException exception) {
            if (exception.errorType() != ErrorType.EXTERNAL_API_ERROR) {
                throw exception;
            }
            return CrawlOnlineItemResult.temporaryFailure(
                    command.targetUrl(), OffsetDateTime.now(), exception.getMessage());
        }
    }
}
