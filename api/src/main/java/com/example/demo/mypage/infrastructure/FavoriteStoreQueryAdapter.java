package com.example.demo.mypage.infrastructure;

import com.example.demo.mypage.application.port.FavoriteStoreQueryPort;
import com.example.demo.mypage.application.query.FavoriteStoresQuery;
import com.example.demo.mypage.application.result.FavoriteStoreSource;
import com.example.demo.report.domain.QStore;
import com.example.demo.store.domain.QStoreFavorite;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class FavoriteStoreQueryAdapter implements FavoriteStoreQueryPort {

    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public List<FavoriteStoreSource> findAll(final FavoriteStoresQuery query) {
        final QStore store = QStore.store;
        final QStoreFavorite favorite = QStoreFavorite.storeFavorite;
        return jpaQueryFactory
                .select(Projections.constructor(
                        FavoriteStoreSource.class,
                        store.id,
                        store.placeName,
                        store.latitude,
                        store.longitude))
                .from(favorite)
                .join(store)
                .on(store.id.eq(favorite.storeId))
                .where(favorite.userId.eq(query.userId()))
                .fetch();
    }
}
