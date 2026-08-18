package com.example.demo.item.application.usecase;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import com.example.demo.item.application.port.ItemExistencePort;
import com.example.demo.item.application.port.PublicPriceQueryPort;
import com.example.demo.item.application.query.ItemPublicPriceQuery;
import com.example.demo.item.application.result.ItemPublicPriceResult;
import com.example.demo.item.application.result.PublicPricePointResult;
import com.example.demo.item.domain.Item;
import com.example.demo.item.domain.PublicPrice;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetItemPublicPriceQueryUseCase {

    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

    private final ItemExistencePort itemExistencePort;
    private final PublicPriceQueryPort publicPriceQueryPort;

    @Transactional(readOnly = true)
    public ItemPublicPriceResult execute(final ItemPublicPriceQuery query) {
        final Item item = findItem(query.itemId());
        final List<PublicPrice> prices =
                publicPriceQueryPort.findByPeriod(query, LocalDate.now(SERVICE_ZONE));
        return new ItemPublicPriceResult(
                item.id(), item.defaultUnit(), query.period(), toPoints(prices));
    }

    private Item findItem(final Long itemId) {
        return itemExistencePort.findById(itemId).orElseThrow(() -> new ApiException(
                ErrorType.NO_RESOURCE_ERROR.description(),
                ErrorType.NO_RESOURCE_ERROR,
                HttpStatus.NOT_FOUND));
    }

    private List<PublicPricePointResult> toPoints(final List<PublicPrice> prices) {
        final Map<LocalDate, Integer> latestPriceByDate = new LinkedHashMap<>();
        prices.forEach(price -> latestPriceByDate.put(price.priceDate(), price.price()));
        return latestPriceByDate.entrySet().stream()
                .map(entry -> new PublicPricePointResult(entry.getKey(), entry.getValue()))
                .toList();
    }
}
