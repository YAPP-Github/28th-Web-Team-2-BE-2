package com.example.demo.item.infrastructure.crawler.oasis;

import com.example.demo.item.application.command.CrawlOnlinePriceCommand;
import com.example.demo.item.application.port.OnlinePriceCrawlerPort;
import com.example.demo.item.application.result.OnlinePriceCrawlResult;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OasisOnlinePriceCrawlerAdapter implements OnlinePriceCrawlerPort {

    private final OasisOnlineItemCrawler itemCrawler;

    @Override
    public List<OnlinePriceCrawlResult> crawl(final CrawlOnlinePriceCommand command) {
        return itemCrawler.crawl(command.itemName()).stream()
                .map(product -> new OnlinePriceCrawlResult(
                        command.itemName(),
                        product.name(),
                        product.pricePer100g(),
                        OnlinePriceCrawlResult.PER_100_GRAMS,
                        product.productUrl(),
                        product.deliveryNote()))
                .toList();
    }
}
