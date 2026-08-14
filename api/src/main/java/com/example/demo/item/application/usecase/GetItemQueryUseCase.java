package com.example.demo.item.application.usecase;

import com.example.demo.item.application.query.ItemQuery;
import com.example.demo.item.application.port.ItemQueryPort;
import com.example.demo.item.application.port.PublicPriceQueryPort;
import com.example.demo.item.application.result.ItemPriceResult;
import com.example.demo.item.application.result.ItemQueryResult;
import com.example.demo.item.domain.Item;
import com.example.demo.item.domain.PublicPrice;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetItemQueryUseCase {

    private final ItemQueryPort itemQueryPort;
    private final PublicPriceQueryPort publicPriceQueryPort;

    @Transactional(readOnly = true)
    public ItemQueryResult execute(final ItemQuery query) {
        final Page<Item> itemPage = itemQueryPort.findAll(PageRequest.of(
                query.page(),
                query.size(),
                Sort.by(Sort.Direction.ASC, "id")));
        final LocalDate baseDate = publicPriceQueryPort.findLatest()
                .map(PublicPrice::priceDate)
                .orElse(null);
        final List<ItemPriceResult> items = itemPage.getContent().stream()
                .map(item -> toResult(item, baseDate))
                .toList();
        return new ItemQueryResult(
                baseDate,
                itemPage.getTotalElements(),
                items,
                query.page(),
                query.size(),
                itemPage.hasNext());
    }

    private ItemPriceResult toResult(final Item item, final LocalDate baseDate) {
        if (baseDate == null) {
            return new ItemPriceResult(item.id(), item.name(), item.imageUrl(), null, null);
        }
        final PublicPrice publicPrice = publicPriceQueryPort
                .findByItemIdAndPriceDate(item.id(), baseDate)
                .orElse(null);
        if (publicPrice == null) {
            return new ItemPriceResult(item.id(), item.name(), item.imageUrl(), null, null);
        }
        return new ItemPriceResult(item.id(), item.name(), item.imageUrl(), publicPrice.price(), null);
    }
}
