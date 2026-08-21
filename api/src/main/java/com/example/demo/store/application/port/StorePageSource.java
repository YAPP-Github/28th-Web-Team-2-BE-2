package com.example.demo.store.application.port;

import com.example.demo.store.application.result.StorePageContent;

public interface StorePageSource {

    StorePageContent find(String placeUrl);
}
