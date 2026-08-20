package com.example.demo.mypage.infrastructure;

import com.example.demo.mypage.application.port.FavoriteStoreQueryPort;
import com.example.demo.mypage.application.query.FavoriteStoresQuery;
import com.example.demo.mypage.application.result.FavoriteStoreSource;
import com.example.demo.report.domain.QStore;
import com.example.demo.store.domain.QStoreFavorite;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class FavoriteStoreQueryAdapter implements FavoriteStoreQueryPort {

    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public Page<FavoriteStoreSource> findAll(final FavoriteStoresQuery query) {
        final QStore store = QStore.store;
        final QStoreFavorite favorite = QStoreFavorite.storeFavorite;
        final List<FavoriteStoreSource> stores = jpaQueryFactory
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
                .orderBy(orderBy(store, query))
                .offset((long) query.page() * query.size())
                .limit(query.size())
                .fetch();
        return new PageImpl<>(
                stores,
                PageRequest.of(query.page(), query.size()),
                count(favorite, store, query));
    }

    private long count(
            final QStoreFavorite favorite,
            final QStore store,
            final FavoriteStoresQuery query) {
        final Long count = jpaQueryFactory
                .select(favorite.count())
                .from(favorite)
                .join(store)
                .on(store.id.eq(favorite.storeId))
                .where(favorite.userId.eq(query.userId()))
                .fetchOne();
        if (count == null) {
            return 0L;
        }
        return count;
    }

    private OrderSpecifier<?>[] orderBy(
            final QStore store, final FavoriteStoresQuery query) {
        if (!query.hasCoordinates()) {
            return new OrderSpecifier<?>[] {store.id.asc()};
        }
        return new OrderSpecifier<?>[] {
            distanceExpression(store, query).asc().nullsLast(), store.id.asc()
        };
    }

    private NumberExpression<Double> distanceExpression(
            final QStore store, final FavoriteStoresQuery query) {
        final NumberExpression<Double> latitudeDifference = Expressions.numberTemplate(
                Double.class,
                "radians({0} - {1}) / 2",
                store.latitude,
                query.latitude());
        final NumberExpression<Double> longitudeDifference = Expressions.numberTemplate(
                Double.class,
                "radians({0} - {1}) / 2",
                store.longitude,
                query.longitude());
        final NumberExpression<Double> queryLatitude = Expressions.numberTemplate(
                Double.class,
                "radians({0})",
                query.latitude());
        final NumberExpression<Double> storeLatitude = Expressions.numberTemplate(
                Double.class,
                "radians({0})",
                store.latitude);
        final NumberExpression<Double> haversine = Expressions.numberTemplate(
                Double.class,
                "{0} * {0} + cos({1}) * cos({2}) * {3} * {3}",
                latitudeDifference,
                queryLatitude,
                storeLatitude,
                longitudeDifference);
        return Expressions.numberTemplate(
                Double.class,
                "6371000 * 2 * atan2(sqrt({0}), sqrt(1 - {0}))",
                haversine);
    }
}
