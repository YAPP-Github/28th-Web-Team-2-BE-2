package com.example.demo.price.application.usecase;

import com.example.demo.price.application.command.CollectionTask;
import com.example.demo.price.application.port.ChannelCrawler;
import com.example.demo.price.application.result.CrawlResult;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CrawlPriceTaskUseCase {

    private final List<ChannelCrawler> crawlers;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public CrawlResult execute(final CollectionTask task) {
        return crawlerFor(task).crawl(task);
    }

    private ChannelCrawler crawlerFor(final CollectionTask task) {
        return crawlers.stream()
                .filter(crawler -> crawler.channel() == task.channel())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("crawler not configured: " + task.channel()));
    }
}
