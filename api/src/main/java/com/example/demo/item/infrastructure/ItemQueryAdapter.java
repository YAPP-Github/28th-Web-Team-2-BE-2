package com.example.demo.item.infrastructure;

import com.example.demo.item.application.port.ItemQueryPort;
import com.example.demo.item.application.query.ItemQuery;
import com.example.demo.item.application.query.ItemSort;
import com.example.demo.item.domain.Item;
import com.example.demo.item.domain.QItem;
import com.example.demo.item.domain.QPublicPrice;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ItemQueryAdapter implements ItemQueryPort {

    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public Page<Item> findAll(final ItemQuery query) {
        final QItem item = QItem.item;
        final QPublicPrice currentPrice = new QPublicPrice("currentPrice");
        final BooleanExpression keywordCondition = keywordCondition(item, query.keyword());
        final JPAQuery<Item> contentQuery = jpaQueryFactory.selectFrom(item).where(keywordCondition);
        if (query.sort() != ItemSort.NAME_ASC) {
            joinCurrentPrice(contentQuery, item, currentPrice, query.regionId());
        }
        final List<Item> content = contentQuery
                .orderBy(orderBy(query.sort(), item, currentPrice))
                .offset((long) query.page() * query.size())
                .limit(query.size())
                .fetch();
        final long totalCount = jpaQueryFactory
                .select(item.count())
                .from(item)
                .where(keywordCondition)
                .fetchOne();
        return new PageImpl<>(content, PageRequest.of(query.page(), query.size()), totalCount);
    }

    private BooleanExpression keywordCondition(final QItem item, final String keyword) {
        if (keyword == null) {
            return null;
        }
        return item.name.contains(keyword);
    }

    private void joinCurrentPrice(
            final JPAQuery<Item> contentQuery,
            final QItem item,
            final QPublicPrice currentPrice,
            final String regionId) {
        final QPublicPrice newerPrice = new QPublicPrice("newerPrice");
        contentQuery.leftJoin(currentPrice).on(
                currentPrice.itemId.eq(item.id),
                currentPrice.regionId.eq(regionId),
                JPAExpressions.selectOne()
                        .from(newerPrice)
                        .where(
                                newerPrice.itemId.eq(currentPrice.itemId),
                                newerPrice.regionId.eq(currentPrice.regionId),
                                newerPrice.priceDate.gt(currentPrice.priceDate)
                                        .or(newerPrice.priceDate.eq(currentPrice.priceDate)
                                                .and(newerPrice.id.gt(currentPrice.id))))
                        .notExists());
    }

    private OrderSpecifier<?>[] orderBy(
            final ItemSort sort,
            final QItem item,
            final QPublicPrice currentPrice) {
        if (sort == ItemSort.NAME_ASC) {
            return nameOrder(item);
        }
        if (sort == ItemSort.PRICE_ASC) {
            return new OrderSpecifier<?>[] {
                currentPrice.price.asc().nullsLast(), item.name.asc(), item.id.asc()
            };
        }
        return new OrderSpecifier<?>[] {
            currentPrice.price.desc().nullsLast(), item.name.asc(), item.id.asc()
        };
    }

    private OrderSpecifier<?>[] nameOrder(final QItem item) {
        return new OrderSpecifier<?>[] {item.name.asc(), item.id.asc()};
    }
}
