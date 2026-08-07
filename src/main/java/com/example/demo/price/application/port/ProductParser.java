package com.example.demo.price.application.port;

import com.example.demo.price.application.command.CollectionTask;
import com.example.demo.price.domain.RawOffer;
import java.util.List;

public interface ProductParser {

    List<RawOffer> parse(String html, CollectionTask task);
}
