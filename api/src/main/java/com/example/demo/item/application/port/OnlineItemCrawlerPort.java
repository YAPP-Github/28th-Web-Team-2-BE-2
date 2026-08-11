package com.example.demo.item.application.port;

import com.example.demo.item.application.command.CrawlOnlineItemCommand;
import com.example.demo.item.application.result.CrawlOnlineItemResult;

public interface OnlineItemCrawlerPort {

    CrawlOnlineItemResult crawl(CrawlOnlineItemCommand command);
}
