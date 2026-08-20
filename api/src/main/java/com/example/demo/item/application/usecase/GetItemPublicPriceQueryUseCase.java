package com.example.demo.item.application.usecase;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import com.example.demo.item.application.port.ItemExistencePort;
import com.example.demo.item.application.port.PublicPriceQueryPort;
import com.example.demo.item.application.query.ItemPublicPriceQuery;
import com.example.demo.item.application.query.PublicPriceRange;
import com.example.demo.item.application.result.ItemPublicPriceResult;
import com.example.demo.item.domain.Item;
import com.example.demo.item.domain.PublicPrice;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 품목의 기간별 공공가격 추이를 조회한다.
 *
 * <p>기준일은 해당 지역의 최신 {@code price_date}다. 품목 목록·상세 API가 응답의 {@code baseDate}로 쓰는 값과 같은
 * 정의이며, 수집이 밀렸을 때 같은 화면의 기준일과 그래프 끝점이 어긋나지 않게 한다.
 */
@Service
@RequiredArgsConstructor
public class GetItemPublicPriceQueryUseCase {

    private final ItemExistencePort itemExistencePort;
    private final PublicPriceQueryPort publicPriceQueryPort;

    @Transactional(readOnly = true)
    public ItemPublicPriceResult execute(final ItemPublicPriceQuery query) {
        final Item item = findItem(query.itemId());
        return new ItemPublicPriceResult(
                item.id(), item.defaultUnit(), query.period(), findPoints(query));
    }

    private Item findItem(final Long itemId) {
        return itemExistencePort.findById(itemId).orElseThrow(() -> new ApiException(
                ErrorType.NO_RESOURCE_ERROR.description(),
                ErrorType.NO_RESOURCE_ERROR,
                HttpStatus.NOT_FOUND));
    }

    private List<ItemPublicPriceResult.Point> findPoints(final ItemPublicPriceQuery query) {
        final LocalDate baseDate =
                publicPriceQueryPort.findLatestPriceDateByRegionId(query.regionId());
        if (baseDate == null) {
            return List.of();
        }
        final PublicPriceRange range = PublicPriceRange.of(query.period(), baseDate);
        return toPoints(publicPriceQueryPort.findByRange(query.itemId(), query.regionId(), range));
    }

    /** 같은 날짜에 가격이 여러 건이면 가장 최근에 저장된 것만 남긴다. 출력은 항상 날짜 오름차순이다. */
    private List<ItemPublicPriceResult.Point> toPoints(final List<PublicPrice> prices) {
        final Map<LocalDate, Integer> latestPriceByDate = new TreeMap<>();
        prices.forEach(price -> latestPriceByDate.put(price.priceDate(), price.price()));
        return latestPriceByDate.entrySet().stream()
                .map(entry -> new ItemPublicPriceResult.Point(entry.getKey(), entry.getValue()))
                .toList();
    }
}
