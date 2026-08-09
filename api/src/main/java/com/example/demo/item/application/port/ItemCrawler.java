package com.example.demo.item.application.port;

import com.example.demo.item.application.command.CrawlRequest;
import com.example.demo.item.application.result.CrawledPage;

public interface ItemCrawler {

    CrawledPage crawl(CrawlRequest request);
}
