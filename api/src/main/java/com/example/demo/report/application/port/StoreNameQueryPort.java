package com.example.demo.report.application.port;

import java.util.Collection;
import java.util.Map;

public interface StoreNameQueryPort {

    /** 가게 ID별 가게명이다. 없는 가게는 결과에 담기지 않는다. */
    Map<Long, String> findNames(Collection<Long> storeIds);
}
