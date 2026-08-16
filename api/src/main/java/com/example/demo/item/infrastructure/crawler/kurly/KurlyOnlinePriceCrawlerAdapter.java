package com.example.demo.item.infrastructure.crawler.kurly;

import com.example.demo.item.application.command.CrawlOnlinePriceCommand;
import com.example.demo.item.application.port.OnlinePriceCrawlerPort;
import com.example.demo.item.application.result.OnlinePriceCrawlResult;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KurlyOnlinePriceCrawlerAdapter implements OnlinePriceCrawlerPort {

    private final KurlyOnlineItemCrawler crawler;

    @Override
    public String channelName() {
        return "컬리";
    }

    @Override
    public List<OnlinePriceCrawlResult> crawl(final CrawlOnlinePriceCommand command) {
        return crawler.crawl(command.itemName()).stream()
                .map(product -> toResult(command.itemName(), product))
                .toList();
    }

    private OnlinePriceCrawlResult toResult(final String itemName, final KurlyProduct product) {
        return new OnlinePriceCrawlResult(
                itemName,
                product.name(),
                product.pricePer100g(),
                OnlinePriceCrawlResult.PER_100_GRAMS,
                product.productUrl(),
                product.deliveryNote());
    }
}
