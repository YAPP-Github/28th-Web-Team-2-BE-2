package com.example.demo.price.application.port;

import com.example.demo.price.domain.ChannelCode;
import com.example.demo.price.application.command.CollectionTask;
import com.example.demo.price.application.result.CrawlResult;

public interface ChannelCrawler {

    CrawlResult crawl(CollectionTask task);

    ChannelCode channel();
}
