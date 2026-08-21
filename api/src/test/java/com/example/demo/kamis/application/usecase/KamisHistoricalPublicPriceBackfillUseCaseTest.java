package com.example.demo.kamis.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.kamis.application.port.KamisItemCatalogPort;
import com.example.demo.kamis.application.port.KamisItemCatalogPort.KamisItem;
import com.example.demo.kamis.application.port.KamisPeriodPriceQueryPort;
import com.example.demo.kamis.application.port.KamisPriceQueryPort;
import com.example.demo.kamis.application.port.PublicPriceCommandPort;
import com.example.demo.kamis.application.port.PublicPriceCommandPort.PublicPriceCommand;
import com.example.demo.kamis.application.query.KamisDailyPriceQuery;
import com.example.demo.kamis.application.query.KamisPeriodPriceQuery;
import com.example.demo.kamis.application.result.KamisDailyPriceItemResult;
import com.example.demo.kamis.application.result.KamisDailyPriceResult;
import com.example.demo.kamis.application.result.KamisPeriodPriceItemResult;
import com.example.demo.kamis.application.result.KamisPeriodPriceResult;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class KamisHistoricalPublicPriceBackfillUseCaseTest {

    private static final String REGION_ID = "1144010200";
    private static final LocalDate START_DATE = LocalDate.of(2025, 8, 21);
    private static final LocalDate END_DATE = LocalDate.of(2026, 8, 20);

    @Test
    void KAMIS_품종과_등급을_일별_응답에서_확인해_1년치_도매가격을_품목ID로_저장한다() {
        final KamisPriceQueryPort dailyPort = mock(KamisPriceQueryPort.class);
        final KamisPeriodPriceQueryPort periodPort = mock(KamisPeriodPriceQueryPort.class);
        final KamisItemCatalogPort catalogPort = mock(KamisItemCatalogPort.class);
        final PublicPriceCommandPort commandPort = mock(PublicPriceCommandPort.class);
        when(catalogPort.findAll()).thenReturn(List.of(
                new KamisItem(1L, "고춧가루-국산", "1kg"),
                new KamisItem(2L, "적상추", "100g")));
        when(dailyPort.findDailyPrices(any(KamisDailyPriceQuery.class)))
                .thenAnswer(invocation -> dailyResult((KamisDailyPriceQuery) invocation.getArgument(0)));
        when(periodPort.findWholesalePeriodPrices(any(KamisPeriodPriceQuery.class)))
                .thenAnswer(invocation -> periodResult((KamisPeriodPriceQuery) invocation.getArgument(0)));
        when(commandPort.upsertAll(any())).thenAnswer(invocation -> invocation.<List<PublicPriceCommand>>getArgument(0).size());

        final KamisHistoricalPublicPriceBackfillUseCase useCase = new KamisHistoricalPublicPriceBackfillUseCase(
                dailyPort, periodPort, catalogPort, commandPort);

        final int saved = useCase.execute(REGION_ID, "1101", START_DATE, END_DATE);

        assertThat(saved).isEqualTo(2);
        final ArgumentCaptor<List<PublicPriceCommand>> commandCaptor = ArgumentCaptor.forClass(List.class);
        verify(commandPort).upsertAll(commandCaptor.capture());
        assertThat(commandCaptor.getValue())
                .extracting(PublicPriceCommand::itemId, PublicPriceCommand::price, PublicPriceCommand::priceDate)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple(1L, 20_000, LocalDate.of(2026, 8, 19)),
                        org.assertj.core.groups.Tuple.tuple(2L, 400, LocalDate.of(2026, 8, 19)));

        final ArgumentCaptor<KamisPeriodPriceQuery> queryCaptor = ArgumentCaptor.forClass(KamisPeriodPriceQuery.class);
        verify(periodPort, org.mockito.Mockito.times(2)).findWholesalePeriodPrices(queryCaptor.capture());
        assertThat(queryCaptor.getAllValues())
                .allSatisfy(query -> {
                    assertThat(query.countryCode()).isEqualTo("1101");
                    assertThat(query.startDay()).isEqualTo(START_DATE);
                    assertThat(query.endDay()).isEqualTo(END_DATE);
                    assertThat(query.productRankCode()).isEqualTo("04");
                    assertThat(query.convertKgYn()).isEqualTo("Y");
                });
    }

    @Test
    void 백필_종료일을_일별_부류_조회일로_전달한다() {
        final KamisPriceQueryPort dailyPort = mock(KamisPriceQueryPort.class);
        when(dailyPort.findDailyPrices(any(KamisDailyPriceQuery.class)))
                .thenReturn(new KamisDailyPriceResult("000", null, List.of()));

        final KamisHistoricalPublicPriceBackfillUseCase useCase = new KamisHistoricalPublicPriceBackfillUseCase(
                dailyPort,
                mock(KamisPeriodPriceQueryPort.class),
                () -> List.of(),
                mock(PublicPriceCommandPort.class));

        assertThat(useCase.execute(REGION_ID, "1101", START_DATE, END_DATE)).isZero();

        final ArgumentCaptor<KamisDailyPriceQuery> queryCaptor = ArgumentCaptor.forClass(KamisDailyPriceQuery.class);
        verify(dailyPort, org.mockito.Mockito.times(6)).findDailyPrices(queryCaptor.capture());
        assertThat(queryCaptor.getAllValues())
                .extracting(KamisDailyPriceQuery::regDay)
                .containsOnly(END_DATE);
    }

    @Test
    void 여러_리전에_저장해도_KAMIS_기간가격은_한번만_조회한다() {
        final KamisPriceQueryPort dailyPort = mock(KamisPriceQueryPort.class);
        final KamisPeriodPriceQueryPort periodPort = mock(KamisPeriodPriceQueryPort.class);
        final KamisItemCatalogPort catalogPort = mock(KamisItemCatalogPort.class);
        final PublicPriceCommandPort commandPort = mock(PublicPriceCommandPort.class);
        when(catalogPort.findAll()).thenReturn(List.of(
                new KamisItem(1L, "고춧가루-국산", "1kg"),
                new KamisItem(2L, "적상추", "100g")));
        when(dailyPort.findDailyPrices(any(KamisDailyPriceQuery.class)))
                .thenAnswer(invocation -> dailyResult((KamisDailyPriceQuery) invocation.getArgument(0)));
        when(periodPort.findWholesalePeriodPrices(any(KamisPeriodPriceQuery.class)))
                .thenAnswer(invocation -> periodResult((KamisPeriodPriceQuery) invocation.getArgument(0)));
        when(commandPort.upsertAll(any())).thenAnswer(invocation -> invocation.<List<PublicPriceCommand>>getArgument(0).size());

        final KamisHistoricalPublicPriceBackfillUseCase useCase = new KamisHistoricalPublicPriceBackfillUseCase(
                dailyPort, periodPort, catalogPort, commandPort);

        final int saved = useCase.execute(List.of("1144010100", "1144010200"), "1101", START_DATE, END_DATE);

        assertThat(saved).isEqualTo(4);
        verify(dailyPort, org.mockito.Mockito.times(6)).findDailyPrices(any(KamisDailyPriceQuery.class));
        verify(periodPort, org.mockito.Mockito.times(2)).findWholesalePeriodPrices(any(KamisPeriodPriceQuery.class));
        final ArgumentCaptor<List<PublicPriceCommand>> commandCaptor = ArgumentCaptor.forClass(List.class);
        verify(commandPort, org.mockito.Mockito.times(2)).upsertAll(commandCaptor.capture());
        assertThat(commandCaptor.getAllValues().stream()
                .flatMap(List::stream)
                .toList())
                .extracting(PublicPriceCommand::itemId, PublicPriceCommand::regionId, PublicPriceCommand::price)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple(1L, "1144010100", 20_000),
                        org.assertj.core.groups.Tuple.tuple(2L, "1144010100", 400),
                        org.assertj.core.groups.Tuple.tuple(1L, "1144010200", 20_000),
                        org.assertj.core.groups.Tuple.tuple(2L, "1144010200", 400));
    }

    @Test
    void 시작일이_종료일보다_늦으면_백필하지_않는다() {
        final KamisHistoricalPublicPriceBackfillUseCase useCase = new KamisHistoricalPublicPriceBackfillUseCase(
                mock(KamisPriceQueryPort.class),
                mock(KamisPeriodPriceQueryPort.class),
                mock(KamisItemCatalogPort.class),
                mock(PublicPriceCommandPort.class));

        assertThatThrownBy(() -> useCase.execute(REGION_ID, "1101", END_DATE, START_DATE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 기간응답의_날짜와_단위가_잘못되면_건너뛰고_상품별_최저가만_저장한다() {
        final KamisPriceQueryPort dailyPort = mock(KamisPriceQueryPort.class);
        final KamisPeriodPriceQueryPort periodPort = mock(KamisPeriodPriceQueryPort.class);
        final KamisItemCatalogPort catalogPort = mock(KamisItemCatalogPort.class);
        final PublicPriceCommandPort commandPort = mock(PublicPriceCommandPort.class);
        when(catalogPort.findAll()).thenReturn(List.of(
                new KamisItem(3L, "감자", "1개"),
                new KamisItem(4L, "감자", "1kg"),
                new KamisItem(5L, "감자", null)));
        when(dailyPort.findDailyPrices(any(KamisDailyPriceQuery.class)))
                .thenAnswer(invocation -> edgeCaseDailyResult((KamisDailyPriceQuery) invocation.getArgument(0)));
        when(periodPort.findWholesalePeriodPrices(any(KamisPeriodPriceQuery.class)))
                .thenReturn(new KamisPeriodPriceResult("000", null, List.of(
                        new KamisPeriodPriceItemResult("감자", "종류", "서울", "시장", null, "20260819", "1,000", null),
                        new KamisPeriodPriceItemResult("감자", "종류", "서울", "시장", "2026", "08/19", "900", "1개"),
                        new KamisPeriodPriceItemResult("감자", "종류", "서울", "시장", "2026", "2026-08-19", "950", "1개"),
                        new KamisPeriodPriceItemResult("감자", "종류", "서울", "시장", "2026", "08/21", "1,000", "1개"),
                        new KamisPeriodPriceItemResult("감자", "종류", "서울", "시장", "2026", "13/99", "1,000", "1개"),
                        new KamisPeriodPriceItemResult("감자", "종류", "서울", "시장", "2026", null, "1,000", "1개"),
                        new KamisPeriodPriceItemResult("감자", "종류", "서울", "시장", "2026", "08/19", "-", "1개"),
                        new KamisPeriodPriceItemResult("감자", "종류", "서울", "시장", "2026", "08/19", "invalid", "1개"),
                        new KamisPeriodPriceItemResult("감자", "종류", "서울", "시장", "2026", "08/19", "1,000", "1개"))));
        when(commandPort.upsertAll(any())).thenAnswer(invocation -> invocation.<List<PublicPriceCommand>>getArgument(0).size());

        final KamisHistoricalPublicPriceBackfillUseCase useCase = new KamisHistoricalPublicPriceBackfillUseCase(
                dailyPort, periodPort, catalogPort, commandPort);

        final int saved = useCase.execute(REGION_ID, "1101", START_DATE, END_DATE);

        assertThat(saved).isEqualTo(1);
        final ArgumentCaptor<List<PublicPriceCommand>> commandCaptor = ArgumentCaptor.forClass(List.class);
        verify(commandPort).upsertAll(commandCaptor.capture());
        assertThat(commandCaptor.getValue())
                .extracting(PublicPriceCommand::itemId, PublicPriceCommand::price)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(3L, 900));
        final ArgumentCaptor<KamisPeriodPriceQuery> queryCaptor = ArgumentCaptor.forClass(KamisPeriodPriceQuery.class);
        verify(periodPort).findWholesalePeriodPrices(queryCaptor.capture());
        assertThat(queryCaptor.getValue().productRankCode()).isEqualTo("05");
    }

    @Test
    void 기간이_1년을_초과하면_백필하지_않는다() {
        final KamisHistoricalPublicPriceBackfillUseCase useCase = new KamisHistoricalPublicPriceBackfillUseCase(
                mock(KamisPriceQueryPort.class),
                mock(KamisPeriodPriceQueryPort.class),
                mock(KamisItemCatalogPort.class),
                mock(PublicPriceCommandPort.class));

        assertThatThrownBy(() -> useCase.execute(
                        REGION_ID, "1101", START_DATE, START_DATE.plusYears(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("KAMIS backfill range must not exceed one year");
    }

    @Test
    void 윤년을_포함한_1년_범위는_백필한다() {
        final KamisPriceQueryPort dailyPort = mock(KamisPriceQueryPort.class);
        final KamisItemCatalogPort catalogPort = mock(KamisItemCatalogPort.class);
        when(catalogPort.findAll()).thenReturn(List.of());
        when(dailyPort.findDailyPrices(any(KamisDailyPriceQuery.class)))
                .thenReturn(new KamisDailyPriceResult("000", null, List.of()));
        final KamisHistoricalPublicPriceBackfillUseCase useCase = new KamisHistoricalPublicPriceBackfillUseCase(
                dailyPort,
                mock(KamisPeriodPriceQueryPort.class),
                catalogPort,
                mock(PublicPriceCommandPort.class));

        assertThat(useCase.execute(
                        REGION_ID,
                        "1101",
                        LocalDate.of(2024, 2, 29),
                        LocalDate.of(2025, 2, 28)))
                .isZero();
    }

    private KamisDailyPriceResult dailyResult(final KamisDailyPriceQuery query) {
        if (!"200".equals(query.itemCategoryCode())) {
            return new KamisDailyPriceResult("000", null, List.of());
        }
        return new KamisDailyPriceResult("000", null, List.of(
                new KamisDailyPriceItemResult(
                        "고춧가루", "248", "국산(1kg)", "01", "상품", "20kg", null, null,
                        null, null, null, null, null, null, null, null, null, null, null, null),
                new KamisDailyPriceItemResult(
                        "상추", "214", "적(100g)", "02", "상품", "4kg", null, null,
                        null, null, null, null, null, null, null, null, null, null, null, null)));
    }

    private KamisPeriodPriceResult periodResult(final KamisPeriodPriceQuery query) {
        final String price = "248".equals(query.itemCode()) ? "20,000" : "4,000";
        return new KamisPeriodPriceResult("000", null, List.of(
                new KamisPeriodPriceItemResult(
                        "고춧가루", "국산(1kg)", "서울", "시장", "2026", "08/19", price, "1kg")));
    }

    private KamisDailyPriceResult edgeCaseDailyResult(final KamisDailyPriceQuery query) {
        if (!"100".equals(query.itemCategoryCode())) {
            return new KamisDailyPriceResult("000", null, List.of());
        }
        return new KamisDailyPriceResult("000", null, List.of(
                new KamisDailyPriceItemResult(
                        "감자", "152", "종류", "01", "중품", "1개", null, null,
                        null, null, null, null, null, null, null, null, null, null, null, null),
                new KamisDailyPriceItemResult(
                        "미등록", "999", "종류", "01", "상품", "1개", null, null,
                        null, null, null, null, null, null, null, null, null, null, null, null),
                new KamisDailyPriceItemResult(
                        null, "998", null, "01", "상품", "1개", null, null,
                        null, null, null, null, null, null, null, null, null, null, null, null),
                new KamisDailyPriceItemResult(
                        "누락", null, "종류", "01", "상품", "1개", null, null,
                        null, null, null, null, null, null, null, null, null, null, null, null),
                new KamisDailyPriceItemResult(
                        "미등록", "997", null, "01", "알수없음", "1개", null, null,
                        null, null, null, null, null, null, null, null, null, null, null, null)));
    }
}
