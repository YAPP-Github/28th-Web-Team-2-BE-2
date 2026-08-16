package com.example.demo.item.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.demo.item.application.command.CrawlOnlinePriceCommand;
import com.example.demo.item.application.port.OnlineChannelQueryPort;
import com.example.demo.item.application.port.OnlineItemQueryPort;
import com.example.demo.item.application.port.OnlinePriceCrawlerPort;
import com.example.demo.item.application.port.OnlinePricePersistencePort;
import com.example.demo.item.application.result.OnlinePriceCollectionResult;
import com.example.demo.item.application.result.OnlinePriceCrawlResult;
import com.example.demo.item.domain.Item;
import com.example.demo.item.domain.OnlineChannel;
import com.example.demo.item.domain.OnlinePrice;
import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class CollectOnlinePriceUseCaseTest {

    private static final LocalDate COLLECTION_DATE = LocalDate.of(2026, 8, 16);

    @Test
    void 마흔여섯개_품목과_네개_채널의_크롤링_결과에서_각_작업별_상위_다섯개만_저장한다() {
        final List<Item> items = itemList(46);
        final List<OnlineChannel> channels = channelList();
        final InMemoryOnlinePricePersistence persistence = new InMemoryOnlinePricePersistence();
        final CollectOnlinePriceUseCase useCase = useCase(items, channels, persistence, crawlers());

        final OnlinePriceCollectionResult result = useCase.execute(COLLECTION_DATE);

        assertThat(result.totalTaskCount()).isEqualTo(184);
        assertThat(result.succeededTaskCount()).isEqualTo(184);
        assertThat(result.failedTaskCount()).isZero();
        assertThat(result.savedPriceCount()).isEqualTo(920);
        assertThat(persistence.savedPrices()).hasSize(184);
        assertThat(persistence.savedPrices().values())
                .allSatisfy(prices -> assertThat(prices).hasSize(5));
        assertThat(persistence.savedPrices().values().stream()
                .flatMap(List::stream)
                .map(OnlinePrice::unit))
                .allMatch(unit -> unit == OnlinePriceCrawlResult.PER_100_GRAMS);
    }

    @Test
    void 결과가_없는_성공_작업은_기존_가격을_삭제한다() {
        final Item item = item(1L, "감자");
        final OnlineChannel channel = channel(1, "오아시스");
        final InMemoryOnlinePricePersistence persistence = new InMemoryOnlinePricePersistence();
        final Scope scope = new Scope(1L, 1, COLLECTION_DATE);
        persistence.save(scope, List.of(price(item.name(), "기존 상품", 300)));
        final CollectOnlinePriceUseCase useCase = useCase(
                List.of(item), List.of(channel), persistence, List.of(new StubCrawler("오아시스", name -> List.of())));

        final OnlinePriceCollectionResult result = useCase.execute(COLLECTION_DATE);

        assertThat(result.succeededTaskCount()).isEqualTo(1);
        assertThat(persistence.deletedScopes()).contains(scope);
        assertThat(persistence.savedPrices()).doesNotContainKey(scope);
    }

    @Test
    void 한_작업이_실패해도_다른_작업은_계속하고_실패한_작업의_기존_가격은_보존한다() {
        final Item failedItem = item(1L, "실패 품목");
        final Item succeededItem = item(2L, "성공 품목");
        final OnlineChannel channel = channel(1, "오아시스");
        final InMemoryOnlinePricePersistence persistence = new InMemoryOnlinePricePersistence();
        final Scope failedScope = new Scope(1L, 1, COLLECTION_DATE);
        final OnlinePrice oldPrice = price("실패 품목", "기존 상품", 300);
        persistence.save(failedScope, List.of(oldPrice));
        final CollectOnlinePriceUseCase useCase = useCase(
                List.of(failedItem, succeededItem),
                List.of(channel),
                persistence,
                List.of(new StubCrawler("오아시스", name -> {
                    if (name.equals("실패 품목")) {
                        throw new IllegalStateException("crawler failure");
                    }
                    return List.of(crawlResult(name, "신규 상품", 200));
                })));

        final OnlinePriceCollectionResult result = useCase.execute(COLLECTION_DATE);

        assertThat(result.totalTaskCount()).isEqualTo(2);
        assertThat(result.succeededTaskCount()).isEqualTo(1);
        assertThat(result.failedTaskCount()).isEqualTo(1);
        assertThat(persistence.savedPrices().get(failedScope)).containsExactly(oldPrice);
        assertThat(persistence.savedPrices()).containsKey(new Scope(2L, 1, COLLECTION_DATE));
    }

    private CollectOnlinePriceUseCase useCase(
            final List<Item> items,
            final List<OnlineChannel> channels,
            final InMemoryOnlinePricePersistence persistence,
            final List<OnlinePriceCrawlerPort> crawlers) {
        final OnlineItemQueryPort itemQueryPort = () -> items;
        final OnlineChannelQueryPort channelQueryPort = () -> channels;
        final ReplaceOnlinePriceUseCase replaceUseCase = new ReplaceOnlinePriceUseCase(persistence);
        return new CollectOnlinePriceUseCase(itemQueryPort, channelQueryPort, crawlers, replaceUseCase);
    }

    private List<OnlinePriceCrawlerPort> crawlers() {
        return channelList().stream()
                .map(channel -> new StubCrawler(channel.name(), name -> List.of(
                        crawlResult(name, "상품 1", 101),
                        crawlResult(name, "상품 2", 102),
                        crawlResult(name, "상품 3", 103),
                        crawlResult(name, "상품 4", 104),
                        crawlResult(name, "상품 5", 105),
                        crawlResult(name, "상품 6", 106))))
                .map(crawler -> (OnlinePriceCrawlerPort) crawler)
                .toList();
    }

    private List<Item> itemList(final int count) {
        final List<Item> items = new ArrayList<>();
        for (long id = 1; id <= count; id++) {
            items.add(item(id, "품목" + id));
        }
        return items;
    }

    private List<OnlineChannel> channelList() {
        return List.of(
                channel(1, "오아시스"),
                channel(2, "컬리"),
                channel(3, "11번가"),
                channel(4, "GS SHOP"));
    }

    private Item item(final Long id, final String name) {
        final Item item = mock(Item.class);
        when(item.id()).thenReturn(id);
        when(item.name()).thenReturn(name);
        return item;
    }

    private OnlineChannel channel(final Integer id, final String name) {
        final OnlineChannel channel = mock(OnlineChannel.class);
        when(channel.id()).thenReturn(id);
        when(channel.name()).thenReturn(name);
        return channel;
    }

    private OnlinePrice price(final String itemName, final String productName, final int value) {
        return new OnlinePrice(
                1L, 1, itemName, productName, value, OnlinePriceCrawlResult.PER_100_GRAMS,
                "https://example.com/old", null, COLLECTION_DATE);
    }

    private static OnlinePriceCrawlResult crawlResult(
            final String itemName, final String productName, final int value) {
        return new OnlinePriceCrawlResult(
                itemName,
                productName,
                BigDecimal.valueOf(value),
                OnlinePriceCrawlResult.PER_100_GRAMS,
                URI.create("https://example.com/" + productName.replace(' ', '-')),
                null);
    }

    private record Scope(Long itemId, Integer channelId, LocalDate collectionDate) {}

    private static final class StubCrawler implements OnlinePriceCrawlerPort {

        private final String channelName;
        private final Function<String, List<OnlinePriceCrawlResult>> results;

        private StubCrawler(
                final String channelName,
                final Function<String, List<OnlinePriceCrawlResult>> results) {
            this.channelName = channelName;
            this.results = results;
        }

        @Override
        public String channelName() {
            return channelName;
        }

        @Override
        public List<OnlinePriceCrawlResult> crawl(final CrawlOnlinePriceCommand command) {
            return results.apply(command.itemName());
        }
    }

    private static final class InMemoryOnlinePricePersistence implements OnlinePricePersistencePort {

        private final Map<Scope, List<OnlinePrice>> savedPrices = new HashMap<>();
        private final List<Scope> deletedScopes = new ArrayList<>();

        @Override
        public void deleteAll(final Long itemId, final Integer channelId, final LocalDate collectionDate) {
            final Scope scope = new Scope(itemId, channelId, collectionDate);
            deletedScopes.add(scope);
            savedPrices.remove(scope);
        }

        @Override
        public void saveAll(final List<OnlinePrice> prices) {
            if (prices.isEmpty()) {
                return;
            }
            final OnlinePrice first = prices.get(0);
            savedPrices.put(
                    new Scope(first.itemId(), first.channelId(), first.createdAt()),
                    List.copyOf(prices));
        }

        private void save(final Scope scope, final List<OnlinePrice> prices) {
            savedPrices.put(scope, List.copyOf(prices));
        }

        private Map<Scope, List<OnlinePrice>> savedPrices() {
            return savedPrices;
        }

        private List<Scope> deletedScopes() {
            return deletedScopes;
        }
    }
}
