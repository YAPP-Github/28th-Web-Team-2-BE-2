package com.example.demo.kamis.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.kamis.application.port.KamisItemCatalogPort;
import com.example.demo.kamis.application.port.KamisItemCatalogPort.KamisItem;
import com.example.demo.kamis.application.port.KamisPriceQueryPort;
import com.example.demo.kamis.application.port.PublicPriceCommandPort;
import com.example.demo.kamis.application.port.PublicPriceCommandPort.PublicPriceCommand;
import com.example.demo.kamis.application.query.KamisDailyPriceQuery;
import com.example.demo.kamis.application.result.KamisDailyPriceItemResult;
import com.example.demo.kamis.application.result.KamisDailyPriceResult;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CollectKamisPublicPriceUseCaseTest {

    private static final LocalDate PRICE_DATE = LocalDate.of(2026, 8, 20);
    private static final String REGION_ID = "1144010200";

    @Test
    void KAMIS_convertKgYn_Y_응답은_원단위가_20kg이어도_kg_환산값을_저장한다() {
        final KamisPriceQueryPort priceQueryPort = org.mockito.Mockito.mock(KamisPriceQueryPort.class);
        final KamisItemCatalogPort itemCatalogPort = org.mockito.Mockito.mock(KamisItemCatalogPort.class);
        final PublicPriceCommandPort commandPort = org.mockito.Mockito.mock(PublicPriceCommandPort.class);
        when(itemCatalogPort.findAll()).thenReturn(List.of(
                new KamisItem(1L, "감자", "1kg"),
                new KamisItem(5L, "당근", "100g")));
        when(priceQueryPort.findDailyPrices(any(KamisDailyPriceQuery.class)))
                .thenReturn(new KamisDailyPriceResult("000", null, List.of(
                        result("감자", "20kg", "37,300", "상품"),
                        result("당근", "20kg", "14,150", "중품"),
                        result("당근", "20kg", "37,000", "상품"),
                        result("없는 품목", "1kg", "1,000", "상품"),
                        result("감자", "20kg", "-", "상품"))));
        when(commandPort.upsertAll(any())).thenAnswer(invocation -> invocation.<List<PublicPriceCommand>>getArgument(0).size());

        final CollectKamisPublicPriceUseCase useCase = new CollectKamisPublicPriceUseCase(
                priceQueryPort, itemCatalogPort, commandPort);

        final int saved = useCase.execute(REGION_ID, "1101", PRICE_DATE);

        assertThat(saved).isEqualTo(2);
        final ArgumentCaptor<List<PublicPriceCommand>> captor = ArgumentCaptor.forClass(List.class);
        verify(commandPort).upsertAll(captor.capture());
        assertThat(captor.getValue())
                .extracting(PublicPriceCommand::itemId, PublicPriceCommand::price,
                        PublicPriceCommand::regionId, PublicPriceCommand::priceDate)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple(1L, 37300, REGION_ID, PRICE_DATE),
                        org.assertj.core.groups.Tuple.tuple(5L, 1415, REGION_ID, PRICE_DATE));
        final ArgumentCaptor<KamisDailyPriceQuery> queryCaptor = ArgumentCaptor.forClass(KamisDailyPriceQuery.class);
        verify(priceQueryPort, times(6)).findDailyPrices(queryCaptor.capture());
        assertThat(queryCaptor.getAllValues())
                .allSatisfy(query -> assertThat(query.convertKgYn()).isEqualTo("Y"));
    }

    private KamisDailyPriceItemResult result(
            final String itemName, final String unit, final String price, final String rank) {
        return new KamisDailyPriceItemResult(
                itemName, null, null, null, rank, unit, "당일", price,
                null, null, null, null, null, null, null, null, null, null, null, null);
    }
}
