package com.example.demo.mypage.application.port;

import com.example.demo.mypage.application.query.FavoriteStoresQuery;
import com.example.demo.mypage.application.result.FavoriteStoreSource;
import java.util.List;

public interface FavoriteStoreQueryPort {

    List<FavoriteStoreSource> findAll(FavoriteStoresQuery query);
}
