package com.example.demo.item.infrastructure.crawler.gsshop;

import com.example.demo.item.application.command.CrawlOnlinePriceCommand;
import com.example.demo.item.application.port.OnlinePriceCrawlerPort;
import com.example.demo.item.application.result.OnlinePriceCrawlResult;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GsShopOnlinePriceCrawlerAdapter implements OnlinePriceCrawlerPort {

    private final GsShopOnlineItemCrawler crawler;

    @Override
    public List<OnlinePriceCrawlResult> crawl(final CrawlOnlinePriceCommand command) {
        return crawler.crawl(command.itemName()).stream()
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
