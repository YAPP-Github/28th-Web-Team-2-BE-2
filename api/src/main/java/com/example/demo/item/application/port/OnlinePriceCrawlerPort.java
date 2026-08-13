package com.example.demo.item.application.port;

import com.example.demo.item.application.command.CrawlOnlinePriceCommand;
import com.example.demo.item.application.result.OnlinePriceCrawlResult;
import java.util.List;

public interface OnlinePriceCrawlerPort {

    List<OnlinePriceCrawlResult> crawl(CrawlOnlinePriceCommand command);
}
