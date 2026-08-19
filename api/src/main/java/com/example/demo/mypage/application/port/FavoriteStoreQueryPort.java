package com.example.demo.mypage.application.port;

import com.example.demo.mypage.application.query.FavoriteStoresQuery;
import com.example.demo.mypage.application.result.FavoriteStoreSource;
import org.springframework.data.domain.Page;

public interface FavoriteStoreQueryPort {

    Page<FavoriteStoreSource> findAll(FavoriteStoresQuery query);
}
